package com.dealmanager.web.dto;

import com.dealmanager.domain.ApprovalAuthority;
import com.dealmanager.domain.Deal;
import com.dealmanager.domain.DealPricingCalculator;
import com.dealmanager.domain.DealPricingSummary;
import com.dealmanager.domain.DealStatus;
import com.dealmanager.domain.PricingLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DealView(
        Long id,
        ClientView client,
        String name,
        DealStatus status,
        LocalDate startDate,
        LocalDate endDate,
        ApprovalAuthority requiredAuthority,
        long version,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<PricingLineView> pricingLines,
        DealPricingSummary summary) {

    public static DealView from(Deal deal) {
        return new DealView(
                deal.getId(),
                ClientView.from(deal.getClient()),
                deal.getName(),
                deal.getStatus(),
                deal.getStartDate(),
                deal.getEndDate(),
                deal.getRequiredAuthority(),
                deal.getVersion() == null ? 0 : deal.getVersion(),
                deal.getCreatedBy(),
                deal.getCreatedAt(),
                deal.getUpdatedAt(),
                deal.getPricingLines().stream().map(PricingLineView::from).toList(),
                DealPricingCalculator.summarise(deal.getPricingLines()));
    }

    public record PricingLineView(Long id, String service, BigDecimal standardPrice, BigDecimal proposedPrice,
                                  long committedVolume) {
        static PricingLineView from(PricingLine line) {
            return new PricingLineView(line.getId(), line.getService(), line.getStandardPrice(),
                    line.getProposedPrice(), line.getCommittedVolume());
        }
    }
}
