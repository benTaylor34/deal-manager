package com.dealmanager.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Volume-weighted deal pricing:
 * {@code discount = 1 − Σ(proposed × volume) / Σ(standard × volume)}.
 *
 * <p>The discount is measured across the whole deal rather than per line, so a deep
 * discount on a small line can be offset by a small discount on a big one. That is a
 * business choice worth stating out loud.
 *
 * <p>The rate keeps 10 decimal places, so a deal at 15.0001% is not rounded down to 15%
 * and slipped under the threshold. Revenue figures are rounded to cents for display.
 */
public final class DealPricingCalculator {

    private static final int RATE_SCALE = 10;
    private static final int MONEY_SCALE = 2;

    private DealPricingCalculator() {
    }

    public static DealPricingSummary summarise(Collection<PricingLine> lines) {
        BigDecimal standard = BigDecimal.ZERO;
        BigDecimal proposed = BigDecimal.ZERO;
        for (PricingLine line : lines) {
            BigDecimal volume = BigDecimal.valueOf(line.getCommittedVolume());
            standard = standard.add(line.getStandardPrice().multiply(volume));
            proposed = proposed.add(line.getProposedPrice().multiply(volume));
        }

        BigDecimal discountRate = standard.signum() == 0
                ? BigDecimal.ZERO
                : BigDecimal.ONE.subtract(proposed.divide(standard, RATE_SCALE, RoundingMode.HALF_EVEN));

        return new DealPricingSummary(
                standard.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN),
                proposed.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN),
                standard.subtract(proposed).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN),
                discountRate);
    }
}
