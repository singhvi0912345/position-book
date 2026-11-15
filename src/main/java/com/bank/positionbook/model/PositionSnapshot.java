package com.bank.positionbook.model;

import java.util.List;

/** Immutable snapshot of a position, safe to hand to callers. */
public record PositionSnapshot(String tradingAccount, String securityId, long quantity, List<DrilldownEntry> events) {
}
