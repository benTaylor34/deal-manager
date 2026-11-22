package com.dealmanager.service;

import com.dealmanager.domain.ApprovalAuthority;
import com.dealmanager.domain.ApprovalPolicy;
import com.dealmanager.domain.AuditAction;
import com.dealmanager.domain.Client;
import com.dealmanager.domain.Deal;
import com.dealmanager.domain.DealAction;
import com.dealmanager.domain.DealPricingCalculator;
import com.dealmanager.domain.DealPricingSummary;
import com.dealmanager.domain.DealRuleViolationException;
import com.dealmanager.domain.DealStateMachine;
import com.dealmanager.domain.DealStatus;
import com.dealmanager.domain.PricingLine;
import com.dealmanager.repository.AuditEventRepository;
import com.dealmanager.repository.ClientRepository;
import com.dealmanager.repository.DealRepository;
import com.dealmanager.web.dto.AuditEventView;
import com.dealmanager.web.dto.CreateDealRequest;
import com.dealmanager.web.dto.DealView;
import com.dealmanager.web.dto.PricingLineRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class DealService {

    private final DealRepository deals;
    private final ClientRepository clients;
    private final AuditEventRepository auditEvents;
    private final AuditService audit;
    private final ApprovalPolicy approvalPolicy;
    private final DealStateMachine stateMachine;
    private final Clock clock;

    public DealService(DealRepository deals, ClientRepository clients, AuditEventRepository auditEvents,
                       AuditService audit, ApprovalPolicy approvalPolicy, DealStateMachine stateMachine,
                       Clock clock) {
        this.deals = deals;
        this.clients = clients;
        this.auditEvents = auditEvents;
        this.audit = audit;
        this.approvalPolicy = approvalPolicy;
        this.stateMachine = stateMachine;
        this.clock = clock;
    }

    public DealView create(CreateDealRequest request, String actor) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new InvalidRequestException("endDate must be after startDate");
        }
        Client client = clients.findById(request.clientId())
                .orElseThrow(() -> new InvalidRequestException("Unknown client " + request.clientId()));

        Instant now = Instant.now(clock);
        Deal deal = new Deal(client, request.name().trim(), request.startDate(), request.endDate(), actor, now);
        if (request.pricingLines() != null) {
            request.pricingLines().forEach(line -> deal.addPricingLine(toEntity(line), now));
        }
        Deal saved = deals.saveAndFlush(deal);

        audit.record(saved, actor, AuditAction.CREATED, null, DealStatus.DRAFT, null,
                Map.of("pricingLines", saved.getPricingLines().size()));
        return DealView.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DealView> list(DealStatus status) {
        List<Deal> result = status == null
                ? deals.findAllByOrderByUpdatedAtDesc()
                : deals.findByStatusOrderByUpdatedAtDesc(status);
        return result.stream().map(DealView::from).toList();
    }

    @Transactional(readOnly = true)
    public DealView get(long id) {
        return DealView.from(load(id));
    }

    public DealView addPricingLine(long id, long expectedVersion, PricingLineRequest request, String actor) {
        Deal deal = loadForUpdate(id, expectedVersion);
        PricingLine line = toEntity(request);
        deal.addPricingLine(line, Instant.now(clock));
        Deal saved = deals.saveAndFlush(deal);

        audit.record(saved, actor, AuditAction.PRICING_LINE_ADDED, saved.getStatus(), saved.getStatus(), null,
                Map.of("service", line.getService(),
                        "standardPrice", line.getStandardPrice().toPlainString(),
                        "proposedPrice", line.getProposedPrice().toPlainString(),
                        "committedVolume", line.getCommittedVolume()));
        return DealView.from(saved);
    }

    /**
     * Submits a draft. Within RM authority the deal is approved straight away; otherwise
     * it waits for the Pricing Committee.
     */
    public DealView submit(long id, long expectedVersion, String comment, String actor) {
        Deal deal = loadForUpdate(id, expectedVersion);
        if (deal.getPricingLines().isEmpty()) {
            throw new DealRuleViolationException("A deal needs at least one pricing line before it can be submitted");
        }

        DealPricingSummary pricing = DealPricingCalculator.summarise(deal.getPricingLines());
        ApprovalAuthority authority = approvalPolicy.requiredAuthority(pricing.discountRate());
        DealStatus from = deal.getStatus();
        DealStatus to = stateMachine.next(from, DealAction.SUBMIT, authority);

        deal.routeTo(authority);
        deal.moveTo(to, Instant.now(clock));
        Deal saved = deals.saveAndFlush(deal);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("discountRate", pricing.discountRate().toPlainString());
        details.put("rmDiscountLimit", approvalPolicy.rmDiscountLimit().toPlainString());
        details.put("requiredAuthority", authority.name());

        if (authority == ApprovalAuthority.RM) {
            String reason = "Discount within RM authority (≤ " + percent(approvalPolicy.rmDiscountLimit()) + ")"
                    + (comment == null || comment.isBlank() ? "" : ": " + comment.trim());
            audit.record(saved, actor, AuditAction.AUTO_APPROVED, from, to, reason, details);
        } else {
            audit.record(saved, actor, AuditAction.SUBMITTED, from, to, blankToNull(comment), details);
        }
        return DealView.from(saved);
    }

    public DealView approve(long id, long expectedVersion, String comment, String actor) {
        return decide(id, expectedVersion, DealAction.APPROVE, AuditAction.APPROVED, comment, actor);
    }

    public DealView reject(long id, long expectedVersion, String comment, String actor) {
        if (comment == null || comment.isBlank()) {
            throw new InvalidRequestException("A comment is required to reject a deal");
        }
        return decide(id, expectedVersion, DealAction.REJECT, AuditAction.REJECTED, comment, actor);
    }

    public DealView revise(long id, long expectedVersion, String comment, String actor) {
        return decide(id, expectedVersion, DealAction.REVISE, AuditAction.REVISED, comment, actor);
    }

    @Transactional(readOnly = true)
    public List<AuditEventView> auditTrail(long id) {
        load(id);
        return auditEvents.findByDealIdOrderByOccurredAtAscIdAsc(id).stream().map(AuditEventView::from).toList();
    }

    private DealView decide(long id, long expectedVersion, DealAction action, AuditAction auditAction,
                            String comment, String actor) {
        Deal deal = loadForUpdate(id, expectedVersion);
        DealStatus from = deal.getStatus();
        DealStatus to = stateMachine.next(from, action, deal.getRequiredAuthority());
        deal.moveTo(to, Instant.now(clock));
        Deal saved = deals.saveAndFlush(deal);
        audit.record(saved, actor, auditAction, from, to, blankToNull(comment), null);
        return DealView.from(saved);
    }

    private Deal load(long id) {
        return deals.findById(id).orElseThrow(() -> new NotFoundException("Deal " + id + " not found"));
    }

    /**
     * {@code @Version} alone only protects against two transactions racing. A user who
     * loaded version 3, went to lunch and then clicked Approve would silently overwrite
     * version 4. Comparing the client's If-Match version closes that gap.
     */
    private Deal loadForUpdate(long id, long expectedVersion) {
        Deal deal = load(id);
        if (!Objects.equals(deal.getVersion(), expectedVersion)) {
            throw new StaleVersionException(expectedVersion, deal.getVersion());
        }
        return deal;
    }

    private static PricingLine toEntity(PricingLineRequest r) {
        return new PricingLine(r.service().trim(), r.standardPrice(), r.proposedPrice(), r.committedVolume());
    }

    private static String percent(java.math.BigDecimal fraction) {
        return fraction.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
