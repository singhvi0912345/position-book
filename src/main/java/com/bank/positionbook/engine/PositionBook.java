package com.bank.positionbook.engine;

import com.bank.positionbook.model.DrilldownEntry;
import com.bank.positionbook.model.Position;
import com.bank.positionbook.model.PositionBookException;
import com.bank.positionbook.model.PositionBookException.ErrorType;
import com.bank.positionbook.model.PositionKey;
import com.bank.positionbook.model.PositionSnapshot;
import com.bank.positionbook.model.EventType;
import com.bank.positionbook.model.TradeEvent;
import com.bank.positionbook.model.TrackedTrade;
import com.bank.positionbook.store.PositionBookStore;

import java.util.Comparator;
import java.util.List;

/**
 * Applies trade events to positions and answers position queries. The business
 * rules live here. Every operation runs inside one store call, so a rule check
 * and the update it guards cannot be interleaved with another request.
 */
public class PositionBook {

    private final PositionBookStore store;

    public PositionBook(PositionBookStore store) {
        this.store = store;
    }

    public DrilldownEntry applyTradeEvent(TradeEvent event) {
        return store.writeExclusively(() ->
                event.eventType() == EventType.CANCEL ? cancelTrade(event) : recordTrade(event));
    }

    public PositionSnapshot find(PositionKey key) {
        return store.readExclusively(() -> store.findPosition(key)
                .map(Position::snapshot)
                .orElseThrow(() -> new PositionBookException(ErrorType.NOT_FOUND,
                        "No position for account " + key.tradingAccount() + " and security " + key.securityId())));
    }

    public List<PositionSnapshot> findAll(String tradingAccount) {
        return store.readExclusively(() -> store.findAllPositions().stream()
                .map(Position::snapshot)
                .filter(snapshot -> tradingAccount == null || snapshot.tradingAccount().equals(tradingAccount))
                .sorted(Comparator.comparing(PositionSnapshot::tradingAccount)
                        .thenComparing(PositionSnapshot::securityId))
                .toList());
    }

    private DrilldownEntry recordTrade(TradeEvent event) {
        if (store.findTrackedTrade(event.id()).isPresent()) {
            throw new PositionBookException(ErrorType.CONFLICT, "Event id " + event.id() + " is already in use");
        }
        TrackedTrade trackedTrade = new TrackedTrade(event);
        store.saveTrackedTrade(trackedTrade);
        return store.getOrCreatePosition(trackedTrade.key()).applyTrade(trackedTrade);
    }

    private DrilldownEntry cancelTrade(TradeEvent event) {
        TrackedTrade targetTrade = store.findTrackedTrade(event.id())
                .orElseThrow(() -> new PositionBookException(ErrorType.NOT_FOUND,
                        "No trade event with id " + event.id() + " to cancel"));
        if (targetTrade.isCancelled()) {
            throw new PositionBookException(ErrorType.CONFLICT, "Event id " + event.id() + " is already cancelled");
        }
        if (event.tradingAccount() != null && !event.tradingAccount().equals(targetTrade.event().tradingAccount())) {
            throw new PositionBookException(ErrorType.BUSINESS_RULE,
                    "Cancel trading account does not match the target event " + event.id());
        }
        if (event.securityId() != null && !event.securityId().equals(targetTrade.event().securityId())) {
            throw new PositionBookException(ErrorType.BUSINESS_RULE,
                    "Cancel security does not match the target event " + event.id());
        }
        return store.getOrCreatePosition(targetTrade.key()).applyCancel(targetTrade, event.id());
    }
}
