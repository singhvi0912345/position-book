package com.bank.positionbook.service;

import com.bank.positionbook.api.dto.TradeEventResponse;
import com.bank.positionbook.api.dto.TradeEventRequest;
import com.bank.positionbook.engine.PositionBook;
import com.bank.positionbook.model.DrilldownEntry;
import com.bank.positionbook.model.EventType;
import com.bank.positionbook.model.TradeEvent;
import org.springframework.stereotype.Service;

@Service
public class TradeEventService {

    private final PositionBook positionBook;

    public TradeEventService(PositionBook positionBook) {
        this.positionBook = positionBook;
    }

    public TradeEventResponse submitTradeEvent(TradeEventRequest request) {
        Long quantity = request.eventType() == EventType.CANCEL ? null : request.quantity();
        TradeEvent event = new TradeEvent(
                request.id(), request.eventType(), request.tradingAccount(), request.securityId(), quantity);
        DrilldownEntry acceptedEvent = positionBook.applyTradeEvent(event);
        return TradeEventResponse.from(acceptedEvent);
    }
}
