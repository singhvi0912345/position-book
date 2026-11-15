package com.bank.positionbook.api.dto;

import com.bank.positionbook.model.DrilldownEntry;

public record TradeEventResponse(long id, String type, String tradingAccount, String securityId,
                            long quantity, boolean cancelled) {

    public static TradeEventResponse from(DrilldownEntry drilldownEntry) {
        return new TradeEventResponse(drilldownEntry.id(), drilldownEntry.type(), drilldownEntry.tradingAccount(),
                drilldownEntry.securityId(), drilldownEntry.quantity(), drilldownEntry.cancelled());
    }
}
