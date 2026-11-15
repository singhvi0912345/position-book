package com.bank.positionbook.store;

import com.bank.positionbook.model.Position;
import com.bank.positionbook.model.PositionKey;
import com.bank.positionbook.model.TrackedTrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Keeps all state in JVM memory. Writes are exclusive and reads are shared.
 * The lock is reentrant, so the individual methods can take it again while the
 * engine holds it for a whole operation.
 */
public class InMemoryPositionBookStore implements PositionBookStore {

    private final Map<Long, TrackedTrade> trackedTradesById = new HashMap<>();
    private final Map<PositionKey, Position> positionsByKey = new HashMap<>();
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();

    @Override
    public <T> T writeExclusively(Supplier<T> action) {
        stateLock.writeLock().lock();
        try {
            return action.get();
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public <T> T readExclusively(Supplier<T> action) {
        stateLock.readLock().lock();
        try {
            return action.get();
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public Optional<TrackedTrade> findTrackedTrade(long eventId) {
        stateLock.readLock().lock();
        try {
            return Optional.ofNullable(trackedTradesById.get(eventId));
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public void saveTrackedTrade(TrackedTrade trackedTrade) {
        stateLock.writeLock().lock();
        try {
            trackedTradesById.put(trackedTrade.id(), trackedTrade);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Position> findPosition(PositionKey key) {
        stateLock.readLock().lock();
        try {
            return Optional.ofNullable(positionsByKey.get(key));
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public Position getOrCreatePosition(PositionKey key) {
        stateLock.writeLock().lock();
        try {
            return positionsByKey.computeIfAbsent(key, Position::new);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public List<Position> findAllPositions() {
        stateLock.readLock().lock();
        try {
            return List.copyOf(positionsByKey.values());
        } finally {
            stateLock.readLock().unlock();
        }
    }
}
