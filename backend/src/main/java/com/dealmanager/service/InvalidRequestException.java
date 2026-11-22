package com.dealmanager.service;

/** Input that fails a cross-field check bean validation cannot express (HTTP 400). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
