package com.bank.positionbook.engine;

import com.bank.positionbook.model.DrilldownEntry;
import com.bank.positionbook.model.PositionBookException;
import com.bank.positionbook.model.PositionKey;
import com.bank.positionbook.model.PositionSnapshot;
import com.bank.positionbook.model.EventType;
import com.bank.positionbook.model.TradeEvent;
import com.bank.positionbook.store.InMemoryPositionBookStore;

import com.bank.positionbook.model.PositionBookException.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PositionBookTest {

    private PositionBook positionBook;

    @BeforeEach
    void setUp() {
        positionBook = new PositionBook(new InMemoryPositionBookStore());
    }

    private static TradeEvent buyEvent(long id, String acc, String sec, long qty) {
        return new TradeEvent(id, EventType.BUY, acc, sec, qty);
    }

    private static TradeEvent sellEvent(long id, String acc, String sec, long qty) {
        return new TradeEvent(id, EventType.SELL, acc, sec, qty);
    }

    private static TradeEvent cancelEvent(long id, String acc, String sec) {
        return new TradeEvent(id, EventType.CANCEL, acc, sec, null);
    }

    private PositionSnapshot snapshotOf(String acc, String sec) {
        return positionBook.find(new PositionKey(acc, sec));
    }

    @Test
    void example1_buyingSameSecurityAccumulates() {
        positionBook.applyTradeEvent(buyEvent(1, "ACC1", "SEC1", 100));
        positionBook.applyTradeEvent(buyEvent(2, "ACC1", "SEC1", 50));

        PositionSnapshot position = snapshotOf("ACC1", "SEC1");
        assertThat(position.quantity()).isEqualTo(150);
        assertThat(position.events()).extracting(DrilldownEntry::id).containsExactly(1L, 2L);
    }

    @Test
    void example2_differentSecuritiesAndAccountsAreKeptSeparate() {
        positionBook.applyTradeEvent(buyEvent(3, "ACC1", "SEC1", 12));
        positionBook.applyTradeEvent(buyEvent(4, "ACC1", "SECXYZ", 50));
        positionBook.applyTradeEvent(buyEvent(5, "ACC2", "SECXYZ", 33));
        positionBook.applyTradeEvent(buyEvent(6, "ACC1", "SEC1", 20));

        assertThat(snapshotOf("ACC1", "SEC1").quantity()).isEqualTo(32);
        assertThat(snapshotOf("ACC1", "SEC1").events()).extracting(DrilldownEntry::id).containsExactly(3L, 6L);
        assertThat(snapshotOf("ACC1", "SECXYZ").quantity()).isEqualTo(50);
        assertThat(snapshotOf("ACC2", "SECXYZ").quantity()).isEqualTo(33);
        assertThat(positionBook.findAll(null)).hasSize(3);
    }

    @Test
    void example3_sellReducesPosition() {
        positionBook.applyTradeEvent(buyEvent(10, "ACC1", "SEC1", 100));
        DrilldownEntry sellEntry = positionBook.applyTradeEvent(sellEvent(11, "ACC1", "SEC1", 50));

        assertThat(sellEntry.type()).isEqualTo("SELL");
        PositionSnapshot position = snapshotOf("ACC1", "SEC1");
        assertThat(position.quantity()).isEqualTo(50);
        assertThat(position.events()).extracting(DrilldownEntry::type).containsExactly("BUY", "SELL");
    }

    @Test
    void example4_cancelRemovesTradeEffectAndKeepsDrilldown() {
        positionBook.applyTradeEvent(buyEvent(21, "ACC1", "SEC1", 100));
        DrilldownEntry cancelEntry = positionBook.applyTradeEvent(cancelEvent(21, "ACC1", "SEC1"));
        positionBook.applyTradeEvent(buyEvent(22, "ACC1", "SEC1", 5));

        assertThat(cancelEntry.type()).isEqualTo("CANCEL");
        assertThat(cancelEntry.quantity()).isZero();
        PositionSnapshot position = snapshotOf("ACC1", "SEC1");
        assertThat(position.quantity()).isEqualTo(5);
        assertThat(position.events()).hasSize(3);
        assertThat(position.events().get(0).cancelled()).isTrue();
        assertThat(position.events().get(1).type()).isEqualTo("CANCEL");
        assertThat(position.events().get(2).cancelled()).isFalse();
    }

    @Test
    void cancelWithoutAccountOrSecurityUsesTheTarget() {
        positionBook.applyTradeEvent(buyEvent(30, "ACC1", "SEC1", 40));
        DrilldownEntry entry = positionBook.applyTradeEvent(cancelEvent(30, null, null));

        assertThat(entry.tradingAccount()).isEqualTo("ACC1");
        assertThat(entry.securityId()).isEqualTo("SEC1");
        assertThat(snapshotOf("ACC1", "SEC1").quantity()).isZero();
    }

    @Test
    void cancelingSellRestoresHoldings() {
        positionBook.applyTradeEvent(buyEvent(31, "ACC1", "SEC1", 100));
        positionBook.applyTradeEvent(sellEvent(32, "ACC1", "SEC1", 60));
        positionBook.applyTradeEvent(cancelEvent(32, "ACC1", "SEC1"));

        assertThat(snapshotOf("ACC1", "SEC1").quantity()).isEqualTo(100);
    }

    @Test
    void sellBeyondHoldingsCreatesShortPosition() {
        positionBook.applyTradeEvent(sellEvent(40, "ACC1", "SEC1", 25));

        assertThat(snapshotOf("ACC1", "SEC1").quantity()).isEqualTo(-25);
    }

    @Test
    void positionWhoseNetIsZeroStillReturnsEvents() {
        positionBook.applyTradeEvent(buyEvent(50, "ACC1", "SEC1", 10));
        positionBook.applyTradeEvent(sellEvent(51, "ACC1", "SEC1", 10));

        PositionSnapshot position = snapshotOf("ACC1", "SEC1");
        assertThat(position.quantity()).isZero();
        assertThat(position.events()).hasSize(2);
    }

    @Test
    void duplicateTradeIdIsRejected() {
        positionBook.applyTradeEvent(buyEvent(60, "ACC1", "SEC1", 10));

        assertKind(() -> positionBook.applyTradeEvent(buyEvent(60, "ACC2", "SEC2", 5)), ErrorType.CONFLICT);
    }

    @Test
    void idStaysReservedAfterCancellation() {
        positionBook.applyTradeEvent(buyEvent(61, "ACC1", "SEC1", 10));
        positionBook.applyTradeEvent(cancelEvent(61, "ACC1", "SEC1"));

        assertKind(() -> positionBook.applyTradeEvent(buyEvent(61, "ACC1", "SEC1", 10)), ErrorType.CONFLICT);
    }

    @Test
    void cancelOfUnknownIdIsNotFound() {
        assertKind(() -> positionBook.applyTradeEvent(cancelEvent(999, "ACC1", "SEC1")), ErrorType.NOT_FOUND);
    }

    @Test
    void cancelingTwiceIsConflict() {
        positionBook.applyTradeEvent(buyEvent(70, "ACC1", "SEC1", 10));
        positionBook.applyTradeEvent(cancelEvent(70, "ACC1", "SEC1"));

        assertKind(() -> positionBook.applyTradeEvent(cancelEvent(70, "ACC1", "SEC1")), ErrorType.CONFLICT);
    }

    @Test
    void cancelOfCancelIsNotAllowed() {
        positionBook.applyTradeEvent(buyEvent(71, "ACC1", "SEC1", 10));
        positionBook.applyTradeEvent(cancelEvent(71, "ACC1", "SEC1"));

        assertKind(() -> positionBook.applyTradeEvent(cancelEvent(71, "ACC1", "SEC1")), ErrorType.CONFLICT);
    }

    @Test
    void cancelWithDifferentAccountIsBusinessRuleViolation() {
        positionBook.applyTradeEvent(buyEvent(80, "ACC1", "SEC1", 10));

        assertKind(() -> positionBook.applyTradeEvent(cancelEvent(80, "ACC2", "SEC1")), ErrorType.BUSINESS_RULE);
        assertThat(snapshotOf("ACC1", "SEC1").quantity()).isEqualTo(10);
    }

    @Test
    void cancelWithDifferentSecurityIsBusinessRuleViolation() {
        positionBook.applyTradeEvent(buyEvent(81, "ACC1", "SEC1", 10));

        assertKind(() -> positionBook.applyTradeEvent(cancelEvent(81, "ACC1", "SEC2")), ErrorType.BUSINESS_RULE);
    }

    @Test
    void findUnknownPositionIsNotFound() {
        assertKind(() -> positionBook.find(new PositionKey("NOPE", "NOPE")), PositionBookException.ErrorType.NOT_FOUND);
    }

    @Test
    void findAllFiltersByAccountAndSortsResults() {
        positionBook.applyTradeEvent(buyEvent(90, "ACC2", "SEC1", 1));
        positionBook.applyTradeEvent(buyEvent(91, "ACC1", "SECB", 2));
        positionBook.applyTradeEvent(buyEvent(92, "ACC1", "SECA", 3));

        List<String> keys = positionBook.findAll(null).stream()
                .map(v -> v.tradingAccount() + "/" + v.securityId())
                .toList();
        assertThat(keys).containsExactly("ACC1/SECA", "ACC1/SECB", "ACC2/SEC1");

        assertThat(positionBook.findAll("ACC1")).hasSize(2);
        assertThat(positionBook.findAll("NOBODY")).isEmpty();
    }

    private static void assertKind(Runnable action, PositionBookException.ErrorType expected) {
        PositionBookException ex = catchThrowableOfType(action::run, PositionBookException.class);
        assertThat(ex).as("expected a PositionBookException").isNotNull();
        assertThat(ex.errorType()).isEqualTo(expected);
    }
}
