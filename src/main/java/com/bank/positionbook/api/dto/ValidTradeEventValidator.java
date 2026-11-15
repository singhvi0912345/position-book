package com.bank.positionbook.api.dto;

import com.bank.positionbook.constants.ApiConstants;
import com.bank.positionbook.model.EventType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidTradeEventValidator implements ConstraintValidator<ValidTradeEvent, TradeEventRequest> {

    @Override
    public boolean isValid(TradeEventRequest request, ConstraintValidatorContext context) {
        if (request == null || request.eventType() == null || request.eventType() == EventType.CANCEL) {
            return true;
        }
        Long quantity = request.quantity();
        boolean valid = notBlank(request.tradingAccount())
                && notBlank(request.securityId())
                && quantity != null
                && quantity >= ApiConstants.MIN_QUANTITY
                && quantity <= ApiConstants.MAX_QUANTITY;
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ApiConstants.TRADE_EVENT_REQUIREMENT_MESSAGE)
                    .addConstraintViolation();
        }
        return valid;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
