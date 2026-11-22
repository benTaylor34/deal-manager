package com.dealmanager.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DealPricingCalculatorTest {

    @Test
    void summarisesAnnualRevenue() {
        List<PricingLine> lines = List.of(
                new PricingLine("ACH", new BigDecimal("0.2500"), new BigDecimal("0.2000"), 120_000),
                new PricingLine("Wires", new BigDecimal("25.0000"), new BigDecimal("20.0000"), 1_200));

        DealPricingSummary s = DealPricingCalculator.summarise(lines);

        // standard: 30,000 + 30,000 = 60,000; proposed: 24,000 + 24,000 = 48,000
        assertThat(s.annualStandardRevenue()).isEqualByComparingTo("60000");
        assertThat(s.annualProposedRevenue()).isEqualByComparingTo("48000");
        assertThat(s.revenueGivenUp()).isEqualByComparingTo("12000");
        assertThat(s.discountRate()).isEqualByComparingTo("0.20");
    }

    @Test
    void discountIsVolumeWeightedNotAnAverageOfLines() {
        // Line discounts are 50% and 0%; a plain average would say 25%.
        List<PricingLine> lines = List.of(
                new PricingLine("Small", new BigDecimal("10"), new BigDecimal("5"), 10),
                new PricingLine("Big", new BigDecimal("10"), new BigDecimal("10"), 990));

        assertThat(DealPricingCalculator.summarise(lines).discountRate()).isEqualByComparingTo("0.005");
    }

    @Test
    void emptyDealHasZeroDiscount() {
        DealPricingSummary s = DealPricingCalculator.summarise(List.of());
        assertThat(s.discountRate()).isEqualByComparingTo("0");
        assertThat(s.annualStandardRevenue()).isEqualByComparingTo("0");
    }

    @Test
    void premiumGivesNegativeDiscount() {
        List<PricingLine> lines = List.of(new PricingLine("FX", new BigDecimal("10"), new BigDecimal("11"), 100));
        assertThat(DealPricingCalculator.summarise(lines).discountRate()).isEqualByComparingTo("-0.10");
    }
}
