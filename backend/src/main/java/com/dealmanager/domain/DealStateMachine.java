package com.dealmanager.domain;

import java.util.Objects;

/**
 * The deal lifecycle. Plain Java, so every (status, action) pair is unit-tested.
 *
 * <pre>
 * DRAFT ──SUBMIT──► APPROVED            (within RM authority: auto-approved)
 * DRAFT ──SUBMIT──► PENDING_COMMITTEE   (needs Pricing Committee)
 * PENDING_COMMITTEE ──APPROVE──► APPROVED
 * PENDING_COMMITTEE ──REJECT───► REJECTED
 * REJECTED ──REVISE──► DRAFT
 * </pre>
 *
 * APPROVED is terminal. Anything not drawn above throws {@link IllegalTransitionException}.
 */
public final class DealStateMachine {

    /**
     * @param authority required only for SUBMIT: decides whether the deal is auto-approved
     *                  or routed to the committee. Ignored for other actions.
     */
    public DealStatus next(DealStatus from, DealAction action, ApprovalAuthority authority) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(action, "action");
        return switch (action) {
            case SUBMIT -> {
                requireFrom(DealStatus.DRAFT, from, action);
                Objects.requireNonNull(authority, "authority is required to submit");
                yield authority == ApprovalAuthority.RM ? DealStatus.APPROVED : DealStatus.PENDING_COMMITTEE;
            }
            case APPROVE -> {
                requireFrom(DealStatus.PENDING_COMMITTEE, from, action);
                yield DealStatus.APPROVED;
            }
            case REJECT -> {
                requireFrom(DealStatus.PENDING_COMMITTEE, from, action);
                yield DealStatus.REJECTED;
            }
            case REVISE -> {
                requireFrom(DealStatus.REJECTED, from, action);
                yield DealStatus.DRAFT;
            }
        };
    }

    public boolean isAllowed(DealStatus from, DealAction action) {
        return switch (action) {
            case SUBMIT -> from == DealStatus.DRAFT;
            case APPROVE, REJECT -> from == DealStatus.PENDING_COMMITTEE;
            case REVISE -> from == DealStatus.REJECTED;
        };
    }

    private static void requireFrom(DealStatus expected, DealStatus actual, DealAction action) {
        if (actual != expected) {
            throw new IllegalTransitionException(actual, action);
        }
    }
}
