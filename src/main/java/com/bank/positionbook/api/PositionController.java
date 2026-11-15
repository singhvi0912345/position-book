package com.bank.positionbook.api;

import com.bank.positionbook.api.dto.PositionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.bank.positionbook.service.PositionQueryService;

import java.util.List;

import static com.bank.positionbook.constants.ApiConstants.ID_PATTERN;
import static com.bank.positionbook.constants.ApiConstants.POSITIONS_PATH;
import static com.bank.positionbook.constants.ApiConstants.POSITIONS_TAG;
import static com.bank.positionbook.constants.ApiConstants.POSITIONS_TAG_DESCRIPTION;
import static com.bank.positionbook.constants.ApiConstants.POSITION_KEY_PATH;
import static com.bank.positionbook.constants.ApiConstants.SECURITY_ID_FORMAT_MESSAGE;
import static com.bank.positionbook.constants.ApiConstants.SECURITY_ID_PARAM;
import static com.bank.positionbook.constants.ApiConstants.TRADING_ACCOUNT_FORMAT_MESSAGE;
import static com.bank.positionbook.constants.ApiConstants.TRADING_ACCOUNT_PARAM;

@Validated
@RestController
@RequestMapping(POSITIONS_PATH)
@Tag(name = POSITIONS_TAG, description = POSITIONS_TAG_DESCRIPTION)
public class PositionController {

    private final PositionQueryService service;

    public PositionController(PositionQueryService service) {
        this.service = service;
    }

    @GetMapping(POSITION_KEY_PATH)
    @Operation(summary = "Get one position",
            description = "Returns the net quantity for the account and security, with the event drilldown.")
    @ApiResponse(responseCode = "200", description = "Position found")
    @ApiResponse(responseCode = "400", description = "Invalid identifier format")
    @ApiResponse(responseCode = "404", description = "No event has ever referenced this account and security")
    public PositionResponse getPosition(
            @Parameter(description = "Trading account", example = "ACC1")
            @PathVariable(TRADING_ACCOUNT_PARAM)
            @Pattern(regexp = ID_PATTERN, message = TRADING_ACCOUNT_FORMAT_MESSAGE)
            String tradingAccount,
            @Parameter(description = "Security identifier", example = "SEC1")
            @PathVariable(SECURITY_ID_PARAM)
            @Pattern(regexp = ID_PATTERN, message = SECURITY_ID_FORMAT_MESSAGE)
            String securityId) {
        return service.getPosition(tradingAccount, securityId);
    }

    @GetMapping
    @Operation(summary = "List positions",
            description = "Returns all positions, optionally filtered by trading account.")
    @ApiResponse(responseCode = "200", description = "Positions returned, sorted by account then security")
    @ApiResponse(responseCode = "400", description = "Invalid account filter")
    public List<PositionResponse> listPositions(
            @Parameter(description = "Optional trading account filter", example = "ACC1")
            @RequestParam(name = TRADING_ACCOUNT_PARAM, required = false)
            @Pattern(regexp = ID_PATTERN, message = TRADING_ACCOUNT_FORMAT_MESSAGE)
            String tradingAccount) {
        return service.listPositions(tradingAccount);
    }
}
