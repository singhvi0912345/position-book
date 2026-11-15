package com.bank.positionbook.engine;

import com.bank.positionbook.model.PositionBookException;
import com.bank.positionbook.model.PositionKey;
import com.bank.positionbook.model.PositionSnapshot;
import com.bank.positionbook.model.EventType;
import com.bank.positionbook.model.TradeEvent;
import com.bank.positionbook.store.InMemoryPositionBookStore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class PositionBookConcurrencyTest {

    private static final int THREADS = 32;
    private static final int EVENTS_PER_THREAD = 1_000;

    @Test
    void concurrentBuysProduceExactTotal() throws Exception {
        PositionBook positionBook = new PositionBook(new InMemoryPositionBookStore());
        AtomicLong eventIds = new AtomicLong(1);
        ExecutorService threadPool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> pendingTasks = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                pendingTasks.add(threadPool.submit(() -> {
                    for (int i = 0; i < EVENTS_PER_THREAD; i++) {
                        positionBook.applyTradeEvent(new TradeEvent(eventIds.getAndIncrement(), EventType.BUY, "ACC1", "SEC1", 1L));
                    }
                }));
            }
            for (Future<?> pendingTask : pendingTasks) {
                pendingTask.get();
            }
        } finally {
            threadPool.shutdownNow();
        }

        PositionSnapshot position = positionBook.find(new PositionKey("ACC1", "SEC1"));
        assertThat(position.quantity()).isEqualTo((long) THREADS * EVENTS_PER_THREAD);
        assertThat(position.events()).hasSize(THREADS * EVENTS_PER_THREAD);
    }

    @Test
    void racingDuplicateIdsAcceptExactlyOne() throws Exception {
        PositionBook positionBook = new PositionBook(new InMemoryPositionBookStore());
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService threadPool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> pendingTasks = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                pendingTasks.add(threadPool.submit(() -> {
                    try {
                        positionBook.applyTradeEvent(new TradeEvent(7L, EventType.BUY, "ACC1", "SEC1", 1L));
                        accepted.incrementAndGet();
                    } catch (PositionBookException e) {
                        rejected.incrementAndGet();
                    }
                }));
            }
            for (Future<?> pendingTask : pendingTasks) {
                pendingTask.get();
            }
        } finally {
            threadPool.shutdownNow();
        }

        assertThat(accepted.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(THREADS - 1);
        assertThat(positionBook.find(new PositionKey("ACC1", "SEC1")).quantity()).isEqualTo(1);
    }
}
