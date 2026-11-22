package com.dealmanager.domain;

import java.math.BigDecimal;

/**
 * Annual revenue view of a deal. Volumes are committed annual volumes.
 *
 * @param discountRate overall discount as a fraction (0.15 = 15%); negative for a premium.
 */
public record DealPricingSummary(
        BigDecimal annualStandardRevenue,
        BigDecimal annualProposedRevenue,
        BigDecimal revenueGivenUp,
        BigDecimal discountRate) {
}
