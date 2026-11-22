package com.dealmanager.domain;

public class IllegalTransitionException extends RuntimeException {

    private final DealStatus from;
    private final DealAction action;

    public IllegalTransitionException(DealStatus from, DealAction action) {
        super("Cannot " + action + " a deal that is " + from);
        this.from = from;
        this.action = action;
    }

    public DealStatus from() {
        return from;
    }

    public DealAction action() {
        return action;
    }
}
