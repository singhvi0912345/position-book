package com.bank.positionbook.api;

import com.bank.positionbook.api.dto.TradeEventRequest;
import com.bank.positionbook.constants.ApiConstants;
import com.bank.positionbook.model.EventType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TradeEventRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<ConstraintViolation<TradeEventRequest>> violationsOf(TradeEventRequest request) {
        return validator.validate(request);
    }

    @Test
    void validBuyHasNoViolations() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.BUY, "ACC1", "SEC1", 100L))).isEmpty();
    }

    @Test
    void validSellHasNoViolations() {
        assertThat(violationsOf(new TradeEventRequest(2L, EventType.SELL, "ACC1", "SEC1", 1L))).isEmpty();
    }

    @Test
    void nullIdIsRejected() {
        assertThat(violationsOf(new TradeEventRequest(null, EventType.BUY, "ACC1", "SEC1", 1L))).isNotEmpty();
    }

    @Test
    void zeroAndNegativeIdsAreRejected() {
        assertThat(violationsOf(new TradeEventRequest(0L, EventType.BUY, "ACC1", "SEC1", 1L))).isNotEmpty();
        assertThat(violationsOf(new TradeEventRequest(-5L, EventType.BUY, "ACC1", "SEC1", 1L))).isNotEmpty();
    }

    @Test
    void nullSideIsRejected() {
        assertThat(violationsOf(new TradeEventRequest(1L, null, "ACC1", "SEC1", 1L))).isNotEmpty();
    }

    @Test
    void accountWithIllegalCharactersIsRejected() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.BUY, "AC C", "SEC1", 1L))).isNotEmpty();
    }

    @Test
    void accountLongerThanTwentyCharactersIsRejected() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.BUY, "A".repeat(ApiConstants.ID_MAX_LENGTH + 1), "SEC1", 1L))).isNotEmpty();
    }

    @Test
    void twentyCharacterIdentifierIsAccepted() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.BUY, "A".repeat(ApiConstants.ID_MAX_LENGTH), "SEC1", 1L))).isEmpty();
    }

    @Test
    void buyWithoutAccountIsRejected() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.BUY, null, "SEC1", 1L))).isNotEmpty();
    }

    @Test
    void sellWithoutSecurityIsRejected() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.SELL, "ACC1", " ", 1L))).isNotEmpty();
    }

    @Test
    void buyWithoutQuantityIsRejected() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.BUY, "ACC1", "SEC1", null))).isNotEmpty();
    }

    @Test
    void zeroQuantityIsRejected() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.BUY, "ACC1", "SEC1", ApiConstants.MIN_QUANTITY - 1))).isNotEmpty();
    }

    @Test
    void quantityAboveMaximumIsRejected() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.BUY, "ACC1", "SEC1", ApiConstants.MAX_QUANTITY + 1))).isNotEmpty();
    }

    @Test
    void maximumQuantityIsAccepted() {
        assertThat(violationsOf(new TradeEventRequest(1L, EventType.BUY, "ACC1", "SEC1", ApiConstants.MAX_QUANTITY))).isEmpty();
    }

    @Test
    void cancelNeedsNoAccountQuantityOrSecurity() {
        assertThat(violationsOf(new TradeEventRequest(21L, EventType.CANCEL, null, null, null))).isEmpty();
    }

    @Test
    void cancelIgnoresQuantity() {
        assertThat(violationsOf(new TradeEventRequest(21L, EventType.CANCEL, "ACC1", "SEC1", 0L))).isEmpty();
    }

    @Test
    void cancelStillChecksIdentifierFormat() {
        assertThat(violationsOf(new TradeEventRequest(21L, EventType.CANCEL, "bad account", null, null))).isNotEmpty();
    }
}
