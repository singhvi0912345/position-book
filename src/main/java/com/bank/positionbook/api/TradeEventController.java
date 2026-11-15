package com.bank.positionbook.api;

import com.bank.positionbook.api.dto.TradeEventRequest;
import com.bank.positionbook.api.dto.TradeEventResponse;
import com.bank.positionbook.service.TradeEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.bank.positionbook.constants.ApiConstants.TRADE_EVENTS_PATH;
import static com.bank.positionbook.constants.ApiConstants.TRADE_EVENTS_TAG;
import static com.bank.positionbook.constants.ApiConstants.TRADE_EVENTS_TAG_DESCRIPTION;

@RestController
@RequestMapping(TRADE_EVENTS_PATH)
@Tag(name = TRADE_EVENTS_TAG, description = TRADE_EVENTS_TAG_DESCRIPTION)
public class TradeEventController {

    private final TradeEventService service;

    public TradeEventController(TradeEventService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a trade event",
            description = "Applies a BUY or SELL to the position, or cancels an earlier BUY or SELL with the same id.")
    @ApiResponse(responseCode = "201", description = "Event accepted")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed body")
    @ApiResponse(responseCode = "404", description = "Cancel target does not exist")
    @ApiResponse(responseCode = "409", description = "Duplicate id, or target already cancelled")
    @ApiResponse(responseCode = "422", description = "Cancel account or security does not match the target")
    public TradeEventResponse submitTradeEvent(@Valid @RequestBody TradeEventRequest request) {
        return service.submitTradeEvent(request);
    }
}
