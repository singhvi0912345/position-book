package com.bank.positionbook.api.dto;

import com.bank.positionbook.constants.ApiConstants;
import com.bank.positionbook.model.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@ValidTradeEvent
@Schema(description = "A BUY, SELL or CANCEL trade event")
public record TradeEventRequest(
        @Schema(description = "Positive event id. A CANCEL reuses the id of the event it cancels.", example = "1")
        @NotNull @Positive
        Long id,

        @Schema(description = "Event type", example = "BUY")
        @NotNull
        EventType eventType,

        @Schema(description = "Required for BUY and SELL. Optional for CANCEL; if supplied it must match the target.",
                example = "ACC1")
        @Pattern(regexp = ApiConstants.ID_PATTERN, message = ApiConstants.TRADING_ACCOUNT_FORMAT_MESSAGE)
        String tradingAccount,

        @Schema(description = "Required for BUY and SELL. Optional for CANCEL; if supplied it must match the target.",
                example = "SEC1")
        @Pattern(regexp = ApiConstants.ID_PATTERN, message = ApiConstants.SECURITY_ID_FORMAT_MESSAGE)
        String securityId,

        @Schema(description = "Required for BUY and SELL, from 1 to 1000000000. Ignored for CANCEL.", example = "100")
        Long quantity) {
}
