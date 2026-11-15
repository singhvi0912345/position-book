package com.bank.positionbook.model;

import java.util.ArrayList;
import java.util.List;

/** Net quantity for one (trading account, security) pair plus its ordered event log. */
public class Position {

    private static final String CANCEL = "CANCEL";

    private final PositionKey key;
    private long netQuantity;
    private final List<DrilldownEntry> drilldownEntries = new ArrayList<>();

    public Position(PositionKey key) {
        this.key = key;
    }

    public DrilldownEntry applyTrade(TrackedTrade trade) {
        netQuantity = Math.addExact(netQuantity, trade.signedQuantity());
        DrilldownEntry drilldownEntry = new DrilldownEntry(
                trade.id(),
                trade.event().eventType().name(),
                trade.event().tradingAccount(),
                trade.event().securityId(),
                trade.event().quantity(),
                false);
        drilldownEntries.add(drilldownEntry);
        return drilldownEntry;
    }

    public DrilldownEntry applyCancel(TrackedTrade targetTrade, long cancelId) {
        netQuantity = Math.subtractExact(netQuantity, targetTrade.signedQuantity());
        targetTrade.markCancelled();
        markOriginalCancelled(targetTrade.id());
        DrilldownEntry cancelDrilldownEntry = new DrilldownEntry(
                cancelId, CANCEL, key.tradingAccount(), key.securityId(), 0L, false);
        drilldownEntries.add(cancelDrilldownEntry);
        return cancelDrilldownEntry;
    }

    public PositionSnapshot snapshot() {
        return new PositionSnapshot(key.tradingAccount(), key.securityId(), netQuantity, List.copyOf(drilldownEntries));
    }

    private void markOriginalCancelled(long id) {
        for (int i = 0; i < drilldownEntries.size(); i++) {
            DrilldownEntry existing = drilldownEntries.get(i);
            if (existing.id() == id && !CANCEL.equals(existing.type()) && !existing.cancelled()) {
                drilldownEntries.set(i, new DrilldownEntry(existing.id(), existing.type(), existing.tradingAccount(),
                        existing.securityId(), existing.quantity(), true));
                return;
            }
        }
    }
}
