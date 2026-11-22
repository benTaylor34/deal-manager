package com.dealmanager.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DealStateMachineTest {

    private final DealStateMachine machine = new DealStateMachine();

    @ParameterizedTest(name = "{0} --{1}/{2}--> {3}")
    @CsvSource({
            "DRAFT,             SUBMIT,  RM,                APPROVED",
            "DRAFT,             SUBMIT,  PRICING_COMMITTEE, PENDING_COMMITTEE",
            "PENDING_COMMITTEE, APPROVE, PRICING_COMMITTEE, APPROVED",
            "PENDING_COMMITTEE, REJECT,  PRICING_COMMITTEE, REJECTED",
            "REJECTED,          REVISE,  PRICING_COMMITTEE, DRAFT"
    })
    void legalTransitions(DealStatus from, DealAction action, ApprovalAuthority authority, DealStatus expected) {
        assertThat(machine.next(from, action, authority)).isEqualTo(expected);
    }

    private static final Set<String> LEGAL = Set.of(
            "DRAFT:SUBMIT", "PENDING_COMMITTEE:APPROVE", "PENDING_COMMITTEE:REJECT", "REJECTED:REVISE");

    /** Every (status, action) pair not in the diagram, generated so none can be forgotten. */
    static Stream<Arguments> illegalPairs() {
        return Arrays.stream(DealStatus.values())
                .flatMap(s -> Arrays.stream(DealAction.values()).map(a -> Arguments.of(s, a)))
                .filter(args -> !LEGAL.contains(args.get()[0] + ":" + args.get()[1]));
    }

    @ParameterizedTest(name = "{0} --{1}--> rejected")
    @MethodSource("illegalPairs")
    void illegalTransitionsAreRejected(DealStatus from, DealAction action) {
        assertThat(machine.isAllowed(from, action)).isFalse();
        assertThatThrownBy(() -> machine.next(from, action, ApprovalAuthority.RM))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(action.name());
    }
}
