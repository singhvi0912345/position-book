package com.bank.positionbook.service;

import com.bank.positionbook.api.dto.PositionResponse;
import com.bank.positionbook.engine.PositionBook;
import com.bank.positionbook.model.PositionKey;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PositionQueryService {

    private final PositionBook positionBook;

    public PositionQueryService(PositionBook positionBook) {
        this.positionBook = positionBook;
    }

    public PositionResponse getPosition(String tradingAccount, String securityId) {
        return PositionResponse.from(positionBook.find(new PositionKey(tradingAccount, securityId)));
    }

    public List<PositionResponse> listPositions(String tradingAccount) {
        return positionBook.findAll(tradingAccount).stream().map(PositionResponse::from).toList();
    }
}
