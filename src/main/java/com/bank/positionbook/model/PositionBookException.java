package com.bank.positionbook.model;

/** Business-rule failure raised by the position book. The API layer maps the kind to an HTTP status. */
public class PositionBookException extends RuntimeException {

    public enum ErrorType {
        NOT_FOUND,
        CONFLICT,
        BUSINESS_RULE
    }

    private final ErrorType errorType;

    public PositionBookException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType errorType() {
        return errorType;
    }
}
