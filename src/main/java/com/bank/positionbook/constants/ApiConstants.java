package com.bank.positionbook.constants;

/**
 * Every constant used by the API layer: paths, parameter names, identifier and
 * quantity rules, validation and error messages, and OpenAPI metadata.
 *
 * <p>Constants used only inside the model, engine or store packages stay there,
 * because those packages must not depend on the API layer.
 */
public final class ApiConstants {

    private ApiConstants() {
    }

    // Parameter and path variable names
    public static final String TRADING_ACCOUNT_PARAM = "tradingAccount";
    public static final String SECURITY_ID_PARAM = "securityId";

    // Paths
    public static final String API_BASE_PATH = "/api/v1";
    public static final String TRADE_EVENTS_PATH = API_BASE_PATH + "/trade-events";
    public static final String POSITIONS_PATH = API_BASE_PATH + "/positions";
    public static final String POSITION_KEY_PATH = "/{" + TRADING_ACCOUNT_PARAM + "}/{" + SECURITY_ID_PARAM + "}";
    public static final String API_DOCS_PATH = "/v3/api-docs";
    public static final String SWAGGER_UI_PATH = "/swagger-ui.html";
    public static final String SWAGGER_UI_INDEX_PATH = "/swagger-ui/index.html";

    // Quantity rules
    public static final long MIN_QUANTITY = 1L;
    public static final long MAX_QUANTITY = 1_000_000_000L;

    // Identifier rules
    public static final int ID_MAX_LENGTH = 20;
    public static final String ID_PATTERN = "^[A-Za-z0-9_-]{1," + ID_MAX_LENGTH + "}$";

    // Validation messages
    public static final String ID_FORMAT_MESSAGE =
            "must be 1-" + ID_MAX_LENGTH + " characters: letters, digits, '-' or '_'";
    public static final String TRADING_ACCOUNT_FORMAT_MESSAGE = TRADING_ACCOUNT_PARAM + " " + ID_FORMAT_MESSAGE;
    public static final String SECURITY_ID_FORMAT_MESSAGE = SECURITY_ID_PARAM + " " + ID_FORMAT_MESSAGE;
    public static final String TRADE_EVENT_REQUIREMENT_MESSAGE = "BUY and SELL require "
            + TRADING_ACCOUNT_PARAM + ", " + SECURITY_ID_PARAM + " and a quantity from "
            + MIN_QUANTITY + " to " + MAX_QUANTITY;

    // Error response (problem detail) text and property names
    public static final String PROBLEM_VALIDATION_DETAIL = "Request validation failed";
    public static final String PROBLEM_MALFORMED_REQUEST_DETAIL =
            "Malformed JSON, or a value is not allowed (side must be BUY, SELL or CANCEL)";
    public static final String PROBLEM_INVALID_PARAMETER_FORMAT = "Parameter '%s' has an invalid value";
    public static final String PROBLEM_UNEXPECTED_DETAIL = "An unexpected error occurred";
    public static final String PROBLEM_ERRORS_PROPERTY = "errors";

    // OpenAPI metadata and tags
    public static final String OPEN_API_TITLE = "Position Book API";
    public static final String OPEN_API_VERSION = "1.0.0";
    public static final String OPEN_API_DESCRIPTION = "Submit BUY, SELL and CANCEL trade events and query "
            + "real-time positions per trading account and security. All state is held in memory.";
    public static final String TRADE_EVENTS_TAG = "Trade events";
    public static final String TRADE_EVENTS_TAG_DESCRIPTION = "Submit BUY, SELL and CANCEL events";
    public static final String POSITIONS_TAG = "Positions";
    public static final String POSITIONS_TAG_DESCRIPTION = "Query real-time positions and their drilldown";
}
