package com.bank.positionbook.model;

public enum EventType {
    BUY(1),
    SELL(-1),
    CANCEL(0);

    private final int sign;

    EventType(int sign) {
        this.sign = sign;
    }

    public int sign() {
        return sign;
    }
}
