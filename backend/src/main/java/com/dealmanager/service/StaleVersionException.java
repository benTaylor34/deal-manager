package com.dealmanager.service;

/** The client edited a version of the deal that is no longer current (HTTP 412). */
public class StaleVersionException extends RuntimeException {

    private final long expected;
    private final long actual;

    public StaleVersionException(long expected, long actual) {
        super("Deal was changed by someone else: you have version " + expected + ", current is " + actual);
        this.expected = expected;
        this.actual = actual;
    }

    public long expected() {
        return expected;
    }

    public long actual() {
        return actual;
    }
}
