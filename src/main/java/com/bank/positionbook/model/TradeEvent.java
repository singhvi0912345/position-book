package com.bank.positionbook.model;

/**
 * A trade event as accepted by the position book. For CANCEL events the
 * quantity is ignored and the account and security may be null; the target
 * event determines them.
 */
public record TradeEvent(long id, EventType eventType, String tradingAccount, String securityId, Long quantity) {
}
