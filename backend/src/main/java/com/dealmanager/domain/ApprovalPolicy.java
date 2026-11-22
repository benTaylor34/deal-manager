package com.dealmanager.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The threshold approval rule. Plain Java, so it is unit-tested without Spring.
 *
 * <p>A discount at or below the RM limit (15% by default) is within RM authority;
 * anything deeper must go to the Pricing Committee. The comparison uses
 * {@link BigDecimal#compareTo}, not {@code equals}: {@code 0.150} and {@code 0.15}
 * are equal amounts but not {@code equals}, because {@code equals} also compares scale.
 */
public final class ApprovalPolicy {

    public static final BigDecimal DEFAULT_RM_DISCOUNT_LIMIT = new BigDecimal("0.15");

    private final BigDecimal rmDiscountLimit;

    public ApprovalPolicy(BigDecimal rmDiscountLimit) {
        Objects.requireNonNull(rmDiscountLimit, "rmDiscountLimit");
        if (rmDiscountLimit.signum() < 0 || rmDiscountLimit.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("RM discount limit must be between 0 and 1");
        }
        this.rmDiscountLimit = rmDiscountLimit;
    }

    /**
     * @param discountRate the deal's overall discount as a fraction (0.15 = 15%). A negative
     *                     rate means the proposed price is above standard (a premium).
     */
    public ApprovalAuthority requiredAuthority(BigDecimal discountRate) {
        Objects.requireNonNull(discountRate, "discountRate");
        return discountRate.compareTo(rmDiscountLimit) <= 0
                ? ApprovalAuthority.RM
                : ApprovalAuthority.PRICING_COMMITTEE;
    }

    public BigDecimal rmDiscountLimit() {
        return rmDiscountLimit;
    }
}
