package com.bank.positionbook.api;

import com.bank.positionbook.constants.ApiConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full HTTP round trips. The context is rebuilt after each test so the in-memory state starts empty. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PositionBookApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions postTradeEvent(String json) throws Exception {
        return mvc.perform(post(ApiConstants.TRADE_EVENTS_PATH).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void example1_buysAccumulateOverHttp() throws Exception {
        postTradeEvent("{\"id\":1,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":100}")
                .andExpect(status().isCreated());
        postTradeEvent("{\"id\":2,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":50}")
                .andExpect(status().isCreated());

        mvc.perform(get(ApiConstants.POSITIONS_PATH + "/ACC1/SEC1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(150))
                .andExpect(jsonPath("$.events", hasSize(2)))
                .andExpect(jsonPath("$.events[0].type").value("BUY"));
    }

    @Test
    void example3_sellReducesPositionOverHttp() throws Exception {
        postTradeEvent("{\"id\":10,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":100}")
                .andExpect(status().isCreated());
        postTradeEvent("{\"id\":11,\"eventType\":\"SELL\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":50}")
                .andExpect(status().isCreated());

        mvc.perform(get(ApiConstants.POSITIONS_PATH + "/ACC1/SEC1"))
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.events[1].type").value("SELL"));
    }

    @Test
    void example4_cancelOverHttp() throws Exception {
        postTradeEvent("{\"id\":21,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":100}")
                .andExpect(status().isCreated());
        postTradeEvent("{\"id\":21,\"eventType\":\"CANCEL\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":0}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CANCEL"))
                .andExpect(jsonPath("$.quantity").value(0));
        postTradeEvent("{\"id\":22,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":5}")
                .andExpect(status().isCreated());

        mvc.perform(get(ApiConstants.POSITIONS_PATH + "/ACC1/SEC1"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.events", hasSize(3)))
                .andExpect(jsonPath("$.events[0].cancelled").value(true))
                .andExpect(jsonPath("$.events[1].type").value("CANCEL"))
                .andExpect(jsonPath("$.events[2].quantity").value(5));
    }

    @Test
    void listReturnsPositionsAndSupportsAccountFilter() throws Exception {
        postTradeEvent("{\"id\":30,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":1}")
                .andExpect(status().isCreated());
        postTradeEvent("{\"id\":31,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC2\",\"securityId\":\"SEC1\",\"quantity\":2}")
                .andExpect(status().isCreated());

        mvc.perform(get(ApiConstants.POSITIONS_PATH)).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get(ApiConstants.POSITIONS_PATH).param(ApiConstants.TRADING_ACCOUNT_PARAM, "ACC2"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quantity").value(2));
    }

    @Test
    void invalidBodyReturnsValidationProblem() throws Exception {
        postTradeEvent("{\"id\":0,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void buyMissingQuantityIsRejected() throws Exception {
        postTradeEvent("{\"id\":40,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSideIsRejectedAsMalformed() throws Exception {
        postTradeEvent("{\"id\":41,\"eventType\":\"HOLD\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        postTradeEvent("{\"id\": ").andExpect(status().isBadRequest());
    }

    @Test
    void duplicateIdReturnsConflict() throws Exception {
        String body = "{\"id\":50,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":1}";
        postTradeEvent(body).andExpect(status().isCreated());
        postTradeEvent(body).andExpect(status().isConflict());
    }

    @Test
    void cancelOfUnknownIdReturnsNotFound() throws Exception {
        postTradeEvent("{\"id\":60,\"eventType\":\"CANCEL\"}").andExpect(status().isNotFound());
    }

    @Test
    void cancelWithMismatchedAccountReturnsUnprocessableEntity() throws Exception {
        postTradeEvent("{\"id\":70,\"eventType\":\"BUY\",\"tradingAccount\":\"ACC1\",\"securityId\":\"SEC1\",\"quantity\":1}")
                .andExpect(status().isCreated());
        postTradeEvent("{\"id\":70,\"eventType\":\"CANCEL\",\"tradingAccount\":\"ACC2\",\"securityId\":\"SEC1\"}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unknownPositionReturnsNotFound() throws Exception {
        mvc.perform(get(ApiConstants.POSITIONS_PATH + "/NOPE/NOPE")).andExpect(status().isNotFound());
    }

    @Test
    void invalidPathIdentifierReturnsBadRequest() throws Exception {
        mvc.perform(get(ApiConstants.POSITIONS_PATH + "/ACC%20X/SEC1")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidAccountFilterReturnsBadRequest() throws Exception {
        mvc.perform(get(ApiConstants.POSITIONS_PATH).param(ApiConstants.TRADING_ACCOUNT_PARAM, "ACC%20X"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openApiDocumentDescribesTheApi() throws Exception {
        mvc.perform(get(ApiConstants.API_DOCS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value(ApiConstants.OPEN_API_TITLE))
                .andExpect(jsonPath("$.paths['" + ApiConstants.TRADE_EVENTS_PATH + "']").exists())
                .andExpect(jsonPath("$.paths['" + ApiConstants.POSITIONS_PATH + ApiConstants.POSITION_KEY_PATH + "']").exists());
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mvc.perform(get(ApiConstants.SWAGGER_UI_INDEX_PATH)).andExpect(status().isOk());
    }
}
