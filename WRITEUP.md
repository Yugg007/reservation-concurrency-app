# Reservation Correctness and Operations Write-up

This write-up describes the implementation currently in the repository. It distinguishes mechanisms present in code from deployment and observability work that remains unverified or incomplete.

## Atomic Seat Decision

PostgreSQL is the system of record. In `ReservationService.reserve`, requested seat IDs are deduplicated and sorted. `SeatRepository.lockSeats` obtains pessimistic row locks in seat-number order, so concurrent requests cannot both make a decision from an unlocked `available` read. The service checks every locked seat, creates one reservation row, then runs `bookIfAvailable`, whose update predicate requires every changed row to still be `available`. A count mismatch throws a domain conflict inside the transaction, rolling back the reservation and any seat updates. Competing requests for a sold seat receive `409 seat_taken`.

For multi-seat requests, all seats are locked in deterministic order and the request is all-or-nothing. It does not partially reserve the seats that happened to be free.

The per-user limit and idempotency lookup are serialized using a transaction-scoped PostgreSQL advisory lock keyed from the user ID. That prevents two concurrent requests for the same user from both passing the limit check based on the same old count.

## Idempotency

The reservation row stores `user_id`, `idempotency_key`, and a canonical request representation (`showId|sorted,seats`) in `request_hash`. The lookup is scoped to user and key. The advisory lock serializes same-user requests before lookup and insert.

- Same user, same key, same request: return the original reservation without changing seats; the controller responds `200` for the replay.
- Same user, same key, different show or seat list: return `409 idempotency_mismatch`.
- If that original reservation was later cancelled, replay returns the original `cancelled` reservation and does not book again. A new booking intent must use a new key.

The request representation is a deterministic string, not a cryptographic digest. A database uniqueness constraint for `(user_id, idempotency_key)` would provide an additional persistence-level guard and should be considered before production.

## Cancellation and Holds

The implemented model is explicit cancellation, not timed holds. The owner-only cancellation path locks the reservation row, changes `booked` to `cancelled`, and releases seats only when they are still `booked` and linked to that reservation. Released seats can be booked again. There is no hold creation or expiration job; `held` exists only in status/count logic.

The API uses `booked` as the persisted and returned seat/reservation status, although the original scenario uses the word `confirmed`.

## Consistency and Availability

The booking transaction depends on PostgreSQL for row locks, advisory locks, and atomic updates. If the database is unavailable, booking cannot safely continue; the service should fail closed rather than accept a booking from stale state. The global exception handler maps common connection failures to `503`.

No partition or high-contention chaos test is checked in. The current k6 script is a hot-seat smoke/load test, not proof of zero 5xx or reconciliation at 20,000 requests. Database pool sizing, connection limits, transaction timeouts, and platform capacity must be measured with the actual deployment before making that claim.

## Identity and Authorization

The filter derives identity from the bearer token; it does not read a user ID from the reservation body. The configured admin token gets the admin role and the reserved `admin` identity. Other non-empty tokens become user IDs, except the literal `admin`, which is rejected to avoid identity collision. This is a demonstration token scheme, not production authentication.

Show details include booking identity for admins and for the matching user. Before public use, verify that this data policy matches product privacy requirements and replace the demo token mechanism with a real authentication system.

## Observability

The controller increments in-process Micrometer counters for bookings and decline events. In the current code, replays also increment `reservations_declined_total` with reason `idempotent_replay`, although a replay is not necessarily a decline. Prometheus registry/export and an availability gauge are not configured, so those counters are not yet a verified scrapeable monitoring interface.

Actuator is present and `/actuator/health` is expected to be available. Explicit readiness/liveness probe configuration has not been verified. Request-ID propagation and structured logs are not implemented. A production alerting setup should page on sustained 5xx/DB connection failures, an unexpected booking-to-decline ratio, reservation latency, and any mismatch between seat totals and persisted seat rows.

## Deployment and Burst Evidence

The repository contains a Dockerfile but no platform deployment manifest or public service URL. The README documents local/container use; the deployed URL and cold-start verification must be added after a real deployment.

`Testing/reserve/run-test.sh` prompts for the show ID, base URL, VU count, and comma-separated seat list. Each VU sends one request for the same seat set using a different user ID and idempotency key. It expects one `201` and the remaining calls to receive `409`. It currently does not cover duplicate-key retries, classify conflicts by reason, separately count 5xx, fetch final show state, or assert seat reconciliation. Do not present its output as evidence for those cases.

The checked-in automated test is a Spring context-load test and requires a reachable configured database. It does not currently assert the concurrency invariants.

## AI Use

AI assistance was used during implementation discussions, code edits, behavior analysis, and drafting this documentation. The requests and desired behavior were directed by the developer. AI-generated changes were iteratively inspected, modified, or reverted during the session. The developer should be prepared to explain the locking, transaction, idempotency, and remaining limitations and to validate the final repository state independently.

## Next Steps

1. Remove and rotate datasource credentials from source control and configure deployment secrets.
2. Add integration tests for same-seat contention, same-key replay, same-key/different-body, multi-seat rollback, concurrent per-user limits, and cancel/rebook.
3. Expand the burst harness to count status/reason distributions and verify final seat reconciliation.
4. Configure and verify readiness/liveness probes, Prometheus metrics and seat gauge, and request-correlated structured logs.
5. Deploy to a public staging service, record the URL, and capture cold-start, health, logs, and load-test evidence.