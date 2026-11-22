package com.dealmanager.service;

import com.dealmanager.domain.ApprovalAuthority;
import com.dealmanager.domain.ApprovalPolicy;
import com.dealmanager.domain.AuditAction;
import com.dealmanager.domain.Client;
import com.dealmanager.domain.Deal;
import com.dealmanager.domain.DealRuleViolationException;
import com.dealmanager.domain.DealStateMachine;
import com.dealmanager.domain.DealStatus;
import com.dealmanager.domain.IllegalTransitionException;
import com.dealmanager.domain.PricingLine;
import com.dealmanager.repository.AuditEventRepository;
import com.dealmanager.repository.ClientRepository;
import com.dealmanager.repository.DealRepository;
import com.dealmanager.web.dto.DealView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    @Mock DealRepository deals;
    @Mock ClientRepository clients;
    @Mock AuditEventRepository auditEvents;
    @Mock AuditService audit;

    private DealService service;

    @BeforeEach
    void setUp() {
        service = new DealService(deals, clients, auditEvents, audit,
                new ApprovalPolicy(new BigDecimal("0.15")), new DealStateMachine(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void submitWithinRmAuthorityIsAutoApprovedAndAudited() {
        Deal deal = draftDeal(new BigDecimal("100"), new BigDecimal("90")); // 10% off
        stubLoad(deal);

        DealView result = service.submit(1L, 0L, "Strategic client", "rm.user");

        assertThat(result.status()).isEqualTo(DealStatus.APPROVED);
        assertThat(result.requiredAuthority()).isEqualTo(ApprovalAuthority.RM);
        verify(audit).record(eq(deal), eq("rm.user"), eq(AuditAction.AUTO_APPROVED),
                eq(DealStatus.DRAFT), eq(DealStatus.APPROVED), any(String.class), anyMap());
    }

    @Test
    void submitAboveThresholdGoesToCommittee() {
        Deal deal = draftDeal(new BigDecimal("100"), new BigDecimal("80")); // 20% off
        stubLoad(deal);

        DealView result = service.submit(1L, 0L, null, "rm.user");

        assertThat(result.status()).isEqualTo(DealStatus.PENDING_COMMITTEE);
        assertThat(result.requiredAuthority()).isEqualTo(ApprovalAuthority.PRICING_COMMITTEE);
        verify(audit).record(eq(deal), eq("rm.user"), eq(AuditAction.SUBMITTED),
                eq(DealStatus.DRAFT), eq(DealStatus.PENDING_COMMITTEE), isNull(), anyMap());
    }

    @Test
    void staleVersionIsRejectedBeforeAnythingChanges() {
        Deal deal = draftDeal(new BigDecimal("100"), new BigDecimal("90"));
        ReflectionTestUtils.setField(deal, "version", 4L);
        when(deals.findById(1L)).thenReturn(Optional.of(deal));

        assertThatThrownBy(() -> service.submit(1L, 3L, null, "rm.user"))
                .isInstanceOf(StaleVersionException.class);

        assertThat(deal.getStatus()).isEqualTo(DealStatus.DRAFT);
        verify(deals, never()).saveAndFlush(any());
        verifyNoInteractions(audit);
    }

    @Test
    void cannotSubmitWithoutPricingLines() {
        Deal deal = deal();
        when(deals.findById(1L)).thenReturn(Optional.of(deal));

        assertThatThrownBy(() -> service.submit(1L, 0L, null, "rm.user"))
                .isInstanceOf(DealRuleViolationException.class);
        verifyNoInteractions(audit);
    }

    @Test
    void cannotApproveADraft() {
        Deal deal = draftDeal(new BigDecimal("100"), new BigDecimal("50"));
        when(deals.findById(1L)).thenReturn(Optional.of(deal));

        assertThatThrownBy(() -> service.approve(1L, 0L, "ok", "committee.user"))
                .isInstanceOf(IllegalTransitionException.class);
        verifyNoInteractions(audit);
    }

    @Test
    void committeeApprovalRecordsWhoAndWhy() {
        Deal deal = draftDeal(new BigDecimal("100"), new BigDecimal("50"));
        deal.moveTo(DealStatus.PENDING_COMMITTEE, NOW);
        stubLoad(deal);

        DealView result = service.approve(1L, 0L, "  Worth it for the FX volume  ", "committee.user");

        assertThat(result.status()).isEqualTo(DealStatus.APPROVED);
        verify(audit).record(deal, "committee.user", AuditAction.APPROVED,
                DealStatus.PENDING_COMMITTEE, DealStatus.APPROVED, "Worth it for the FX volume", null);
    }

    @Test
    void rejectRequiresAComment() {
        assertThatThrownBy(() -> service.reject(1L, 0L, "   ", "committee.user"))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(deals, audit);
    }

    @Test
    void cannotAddLinesOnceSubmitted() {
        Deal deal = draftDeal(new BigDecimal("100"), new BigDecimal("50"));
        deal.moveTo(DealStatus.PENDING_COMMITTEE, NOW);
        when(deals.findById(1L)).thenReturn(Optional.of(deal));

        assertThatThrownBy(() -> service.addPricingLine(1L, 0L,
                new com.dealmanager.web.dto.PricingLineRequest("FX", BigDecimal.TEN, BigDecimal.ONE, 5L), "rm.user"))
                .isInstanceOf(DealRuleViolationException.class);
    }

    private void stubLoad(Deal deal) {
        when(deals.findById(1L)).thenReturn(Optional.of(deal));
        when(deals.saveAndFlush(deal)).thenReturn(deal);
    }

    private static Deal deal() {
        Client client = new Client("Northwind Manufacturing", "Commercial");
        ReflectionTestUtils.setField(client, "id", 7L);
        Deal deal = new Deal(client, "2027 cash management", LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 12, 31), "rm.user", NOW);
        ReflectionTestUtils.setField(deal, "id", 1L);
        ReflectionTestUtils.setField(deal, "version", 0L);
        return deal;
    }

    private static Deal draftDeal(BigDecimal standard, BigDecimal proposed) {
        Deal deal = deal();
        deal.addPricingLine(new PricingLine("Wires", standard, proposed, 1000), NOW);
        return deal;
    }
}
