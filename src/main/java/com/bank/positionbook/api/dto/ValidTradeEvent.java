package com.bank.positionbook.api.dto;

import com.bank.positionbook.constants.ApiConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Cross-field rule: BUY and SELL need an account, a security and a valid quantity. */
@Documented
@Constraint(validatedBy = ValidTradeEventValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTradeEvent {

    String message() default ApiConstants.TRADE_EVENT_REQUIREMENT_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
