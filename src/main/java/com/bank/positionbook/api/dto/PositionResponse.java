package com.bank.positionbook.api.dto;

import com.bank.positionbook.model.PositionSnapshot;

import java.util.List;

public record PositionResponse(String tradingAccount, String securityId, long quantity,
                               List<TradeEventResponse> events) {

    public static PositionResponse from(PositionSnapshot snapshot) {
        return new PositionResponse(snapshot.tradingAccount(), snapshot.securityId(), snapshot.quantity(),
                snapshot.events().stream().map(TradeEventResponse::from).toList());
    }
}
