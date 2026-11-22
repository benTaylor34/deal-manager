package com.dealmanager.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalPolicyTest {

    private final ApprovalPolicy policy = new ApprovalPolicy(ApprovalPolicy.DEFAULT_RM_DISCOUNT_LIMIT);

    @ParameterizedTest(name = "discount {0} → {1}")
    @CsvSource({
            "0,            RM",
            "0.10,         RM",
            "0.15,         RM",                 // exactly at the limit stays with the RM
            "0.150,        RM",                 // different scale, same amount: compareTo, not equals
            "0.1500000000, RM",
            "0.1500000001, PRICING_COMMITTEE",  // the tiniest amount over goes to committee
            "0.1501,       PRICING_COMMITTEE",
            "0.40,         PRICING_COMMITTEE",
            "1,            PRICING_COMMITTEE",
            "-0.05,        RM"                  // premium (priced above standard)
    })
    void routesByDiscount(String discount, ApprovalAuthority expected) {
        assertThat(policy.requiredAuthority(new BigDecimal(discount))).isEqualTo(expected);
    }

    @Test
    @DisplayName("the limit is configurable")
    void customLimit() {
        ApprovalPolicy strict = new ApprovalPolicy(new BigDecimal("0.10"));
        assertThat(strict.requiredAuthority(new BigDecimal("0.12"))).isEqualTo(ApprovalAuthority.PRICING_COMMITTEE);
    }

    @Test
    void rejectsNonsenseLimits() {
        assertThatThrownBy(() -> new ApprovalPolicy(new BigDecimal("-0.01"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalPolicy(new BigDecimal("1.01"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("end to end with the calculator: a deal at exactly 15% weighted discount stays with the RM")
    void weightedDiscountAtBoundary() {
        // standard 100×1000 + 50×2000 = 200,000; proposed 80×1000 + 45×2000 = 170,000 → 15.00%
        List<PricingLine> lines = List.of(
                new PricingLine("Wires", new BigDecimal("100.00"), new BigDecimal("80.00"), 1000),
                new PricingLine("Cheques", new BigDecimal("50.00"), new BigDecimal("45.00"), 2000));

        DealPricingSummary summary = DealPricingCalculator.summarise(lines);

        assertThat(summary.discountRate()).isEqualByComparingTo("0.15");
        assertThat(policy.requiredAuthority(summary.discountRate())).isEqualTo(ApprovalAuthority.RM);
    }

    @Test
    @DisplayName("one cent more off tips the deal to the committee")
    void justOverBoundary() {
        List<PricingLine> lines = List.of(
                new PricingLine("Wires", new BigDecimal("100.00"), new BigDecimal("79.99"), 1000),
                new PricingLine("Cheques", new BigDecimal("50.00"), new BigDecimal("45.00"), 2000));

        DealPricingSummary summary = DealPricingCalculator.summarise(lines);

        assertThat(summary.discountRate()).isGreaterThan(new BigDecimal("0.15"));
        assertThat(policy.requiredAuthority(summary.discountRate())).isEqualTo(ApprovalAuthority.PRICING_COMMITTEE);
    }
}
