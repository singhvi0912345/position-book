package com.bank.positionbook.model;

/** Mutable index entry for a BUY or SELL event. Only changed under the book's write lock. */
public final class TrackedTrade {

    private final TradeEvent event;
    private boolean cancelled;

    public TrackedTrade(TradeEvent event) {
        this.event = event;
    }

    public TradeEvent event() {
        return event;
    }

    public long id() {
        return event.id();
    }

    public PositionKey key() {
        return new PositionKey(event.tradingAccount(), event.securityId());
    }

    public long signedQuantity() {
        return event.eventType().sign() * event.quantity();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    void markCancelled() {
        this.cancelled = true;
    }
}
