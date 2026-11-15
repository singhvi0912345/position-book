POSITION BOOK APPLICATION - ASSUMPTIONS
=======================================

This file lists every assumption made while building the Position Book
application. For each point it gives the reason for the choice and, where
relevant, the code that enforces it. The specification leaves these points
open, so each one should be confirmed with the interviewer before the
solution is considered final.


ASSUMPTIONS
-----------

1. The system keeps all data in memory.
   - Reason: the specification requires it. The specification says data must
     not be kept in a database or the file system.
   - Enforced in: store/InMemoryPositionBookStore.java, which holds plain Java
     HashMaps.
   - The build has no database, JDBC, JPA or H2 dependency. The application
     writes no files.
   - Consequence: all data is lost when the application stops (see point 12).

2. Event IDs are positive 64-bit integers.
   - BUY and SELL events must have IDs that are unique across the whole
     Position Book, not only within one trading account.
   - A CANCEL event reuses the ID of the event it cancels.
   - Reason: Example 4 submits BUY 21 and then CANCEL 21, so the cancel must
     carry the same ID as its target. Global uniqueness lets every cancel
     resolve to exactly one trade without needing the account in the request.
   - Enforced in: engine/PositionBook.java (recordTrade and cancelTrade).

3. A BUY or SELL whose ID is already in use is rejected with HTTP 409 Conflict.
   - The ID stays reserved after the original event is cancelled.
   - Reason: if a cancelled ID could be reused, a later CANCEL with that ID
     would be ambiguous. Keeping IDs reserved means every cancel refers to
     exactly one trade for the life of the application.
   - Enforced in: engine/PositionBook.java (recordTrade).

4. A CANCEL must refer to an existing BUY or SELL, and that target must not
   already be cancelled.
   - A missing target returns HTTP 404 Not Found.
   - A second cancel of the same target returns HTTP 409 Conflict.
   - Reason: a cancel with no target has no effect and cannot be recorded. A
     second cancel would reverse the quantity twice and corrupt the position.
   - Enforced in: engine/PositionBook.java (cancelTrade).

5. A CANCEL cannot target another CANCEL.
   - Cancels are not stored as trade targets, so a cancel of a cancel is
     treated as a cancel of an already-cancelled ID and returns HTTP 409.
   - Reason: follows from point 4. Cancellation is not reversible in this
     specification.

6. The quantity on a CANCEL is ignored.
   - The trading account and security ID on a CANCEL are optional. If they
     are supplied, they must match the target's values, otherwise the request
     is rejected with HTTP 422 Unprocessable Entity. If they are omitted, the
     target's values are used.
   - Reason: the specification says the quantity and security ID of a cancel
     are meaningless, yet Example 4 supplies them (ACC1 SEC1 0). Accepting
     them and checking them when present lets the examples work and catches
     client mistakes without making the fields mandatory. Rejecting a mismatch,
     rather than silently correcting it, avoids cancelling the wrong position.
   - Enforced in: api/dto/TradeEventRequest.java (format only),
     api/dto/ValidTradeEventValidator.java (a CANCEL is exempt from the BUY and
     SELL rules) and engine/PositionBook.java (the mismatch checks).

7. Net positions may be negative.
   - A SELL larger than the current holding is accepted and creates a short
     position.
   - Reason: securities can be sold short, and the specification does not
     forbid it.
   - To change this, reject the SELL in engine/PositionBook.java when the
     resulting net would be below zero.

8. For BUY and SELL, the quantity must be a whole number from 1 to
   1,000,000,000 inclusive.
   - Reason: zero and negative quantities have no meaning for a buy or sell.
     The upper bound is far below the 64-bit overflow point and blocks
     clearly erroneous input.
   - Enforced in: api/ApiConstants.java (MIN_QUANTITY and MAX_QUANTITY), used
     by api/dto/ValidTradeEventValidator.java.

9. GET of a position returns HTTP 404 when no event has ever referenced the
   trading account and security pair.
   - A pair whose events net to zero returns HTTP 200 with quantity 0 and its
     drilldown.
   - Reason: this separates "never traded" from "traded and now flat", which
     is useful information in a position book.

10. The drilldown lists every event for the pair in arrival order.
    - It includes the original BUY or SELL, marked cancelled=true once it is
      cancelled, and each CANCEL event.
    - A CANCEL event is reported with quantity 0 and the target's trading
      account and security ID.
    - Reason: Example 4 shows both the cancelled BUY and the CANCEL record in
      its output. Keeping both gives a complete audit trail.

11. Trading account and security IDs are 1 to 20 characters.
    - Allowed characters are letters, digits, hyphen and underscore. The IDs
      are case-sensitive.
    - Reason: a bounded, simple character set is safe to use as map keys and
      URL path segments, and avoids encoding problems. The examples (ACC1,
      SEC1, SECXYZ) all fit. The length limit is a choice, not a requirement.
    - Enforced in: api/ApiConstants.java (ID_PATTERN and ID_MAX_LENGTH).

12. The application runs as a single instance in a single JVM.
    - Restarting it discards all state.
    - Reason: consistent with point 1. Several instances would each hold a
      separate, inconsistent Position Book.

13. Events are applied in the order they are received.
    - No timestamps are required or stored.
    - Reason: the specification does not supply timestamps. The write lock
      serialises concurrent submissions, so the order is well defined.

14. Validation runs before any state changes.
    - A request that fails validation leaves the Position Book untouched.
    - Reason: a partly applied event would corrupt the position. Validation at
      the boundary, plus the rule checks inside the lock, ensures each accepted
      event is applied completely or not at all.

15. Writes are serialised with one ReentrantReadWriteLock, held by the store.
    Reads share the lock.
    - Reason: the simplest model that keeps event IDs globally unique and makes
      each CANCEL atomic with the trade it targets. Throughput is limited to one
      writer at a time, which is acceptable for this exercise.
    - The concurrency tests confirm that 32,000 concurrent BUYs give the exact
      total, and that racing duplicate IDs accept exactly one event.
    - Enforced in: store/InMemoryPositionBookStore.java, used by
      engine/PositionBook.java.

16. Errors are returned as RFC 7807 Problem Details
    (application/problem+json).
    - Reason: a standard format that clients can parse consistently and that
      Spring Boot supports natively.
    - Enforced in: api/error/GlobalExceptionHandler.java.


OPEN QUESTIONS FOR THE INTERVIEWER
----------------------------------

These questions would change the design if the answers differ from the
assumptions above.

  Q1. Should short (negative) positions be allowed? (point 7)
  Q2. Should a never-traded position return 404 or a zero-quantity position?
      (point 9)
  Q3. Should a CANCEL with a mismatched account or security be rejected or
      silently corrected? (point 6)
  Q4. Are event IDs unique across all accounts, or only within one account?
      (point 2)
  Q5. What throughput and latency are expected? A single writer lock is
      sufficient for the exercise but may not be for production. (point 15)
  Q6. Is authentication required? The current solution has none.