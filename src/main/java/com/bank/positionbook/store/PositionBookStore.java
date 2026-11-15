package com.bank.positionbook.store;

import com.bank.positionbook.model.Position;
import com.bank.positionbook.model.PositionKey;
import com.bank.positionbook.model.TrackedTrade;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Storage contract for the position book. The engine runs every business operation
 * inside writeExclusively or readExclusively, so each check and update is atomic.
 */
public interface PositionBookStore {

    <T> T writeExclusively(Supplier<T> action);

    <T> T readExclusively(Supplier<T> action);

    Optional<TrackedTrade> findTrackedTrade(long eventId);

    void saveTrackedTrade(TrackedTrade trackedTrade);

    Optional<Position> findPosition(PositionKey key);

    Position getOrCreatePosition(PositionKey key);

    List<Position> findAllPositions();
}
