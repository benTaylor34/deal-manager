package com.dealmanager.domain;

/** A request that is well-formed but breaks a business rule (HTTP 422). */
public class DealRuleViolationException extends RuntimeException {

    public DealRuleViolationException(String message) {
        super(message);
    }
}
