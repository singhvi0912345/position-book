package com.bank.positionbook.model;

/** One line of a position's drilldown. */
public record DrilldownEntry(long id, String type, String tradingAccount, String securityId,
                            long quantity, boolean cancelled) {
}
