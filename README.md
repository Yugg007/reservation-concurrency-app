# Reservation Service

A Spring Boot and PostgreSQL JSON API for creating shows, booking seats, and cancelling reservations. PostgreSQL is the system of record. This README documents the routes and behavior currently implemented in this repository.

## Current Status

- The API and Dockerfile are in the repository.
- No public deployment URL or deployment-platform configuration is checked in. Add the real URL here after deployment; do not treat `localhost` as a public endpoint.
- The load script exercises concurrent contention on the same seat list. It does not yet report a final seat reconciliation or categorize all decline reasons.
- Prometheus export, explicit liveness/readiness probes, and correlation-ID structured logging are not configured yet. See [WRITEUP.md](WRITEUP.md) for the current gaps.

## Requirements

- Java 17
- PostgreSQL reachable by the application
- Docker, if running the container
- k6, if running the concurrency script

## Configure and Run

Set the datasource and admin token for your environment. Spring Boot environment variables override values in `application.properties`:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/reservation'
export SPRING_DATASOURCE_USERNAME='reservation_user'
export SPRING_DATASOURCE_PASSWORD='replace-me'
export APP_ADMIN_TOKEN='replace-with-a-long-random-token'
```

Then run the application:

```bash
./mvnw spring-boot:run
```

Build and run the tests with:

```bash
./mvnw clean package
./mvnw test
```

The current test suite contains a Spring application-context smoke test; it is not a high-contention correctness test and needs the configured database.

### Docker

```bash
docker build -t reservation:local .
docker run --rm -p 8080:8080 \
	-e SPRING_DATASOURCE_URL="$SPRING_DATASOURCE_URL" \
	-e SPRING_DATASOURCE_USERNAME="$SPRING_DATASOURCE_USERNAME" \
	-e SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
	-e APP_ADMIN_TOKEN="$APP_ADMIN_TOKEN" \
	reservation:local
```

The container listens on port `8080`. It still requires a reachable PostgreSQL database.

## Authentication

Every API request except `/actuator/**` requires `Authorization: Bearer <token>`.

- The token configured by `APP_ADMIN_TOKEN` is the admin identity and can create shows.
- Any other non-empty token is used directly as the user ID. For example, `Bearer buyer-123` identifies user `buyer-123`.
- The non-admin token `admin` is rejected because that ID is reserved for the admin principal.

This is a simple demo token scheme, not production-grade authentication. Do not expose it publicly without replacing it with a proper identity provider and authorization policy. Never commit database credentials or real tokens; rotate any secret that has already been committed.

## API

All prices are integer paise. Multi-seat reservations are all-or-nothing: either every requested seat is booked or none are.

### Create a show

The implemented route is `POST /create/show` (admin token required):

```bash
curl -i http://localhost:8080/create/show \
	-H "Authorization: Bearer $APP_ADMIN_TOKEN" \
	-H 'Content-Type: application/json' \
	-d '{"name":"friday-night","seats":["A1","A2","A3"],"price_paise":25000,"per_user_limit":4}'
```

`per_user_limit` is optional and defaults to 4. The response includes `showId`, integer `price_paise`, seat counts, and a seat map. Each seat has a `status`; null `booked_by` and `reservationId` fields are omitted.

### Read show state

- `GET /show/{showId}` returns one show.
- `GET /shows` returns all shows.

Both routes require a bearer token. Seat statuses in this implementation are `available`, `held`, and `booked`; new bookings use `booked` (not `confirmed`). The response includes `available_seats`, `held_seats`, `booked_seats`, and a per-seat map. Admin requests can see all booking identities; a regular user can see booking details for seats matching their token-derived user ID.

```bash
curl http://localhost:8080/show/$SHOW_ID \
	-H 'Authorization: Bearer buyer-123'
```

### Reserve seats

The implemented route is `POST /shows/reserve/{showId}`. Send the idempotency key in the header or request body; when both are present, the header is used.

```bash
curl -i "http://localhost:8080/shows/reserve/$SHOW_ID" \
	-H 'Authorization: Bearer buyer-123' \
	-H 'Idempotency-Key: buyer-123-order-001' \
	-H 'Content-Type: application/json' \
	-d '{"seats":["A1","A2"]}'
```

A successful new reservation returns `201` and a response containing `reservationId`, `show_id`, `user_id`, `seats`, `amount_paise`, and `status: "booked"`. An exact retry with the same user, key, and request returns the original reservation with `200`. Reusing a key for a different show or seat list returns `409`. After cancellation, the old key still identifies the original cancelled reservation; use a new key for a new booking.

Typical domain declines use `409`, including `seat_taken`, `per_user_limit`, and `idempotency_mismatch`. Unknown seats return `404`.

### Cancel a reservation

Only the reservation owner can cancel it. A successful cancellation marks the reservation cancelled and releases its seats to `available`.

```bash
curl -i -X POST "http://localhost:8080/reservations/cancel/$RESERVATION_ID" \
	-H 'Authorization: Bearer buyer-123'
```

### Health

Spring Boot Actuator is included. Check local health with:

```bash
curl http://localhost:8080/actuator/health
```

This repository does not explicitly configure Kubernetes liveness/readiness probe groups. Verify the health endpoint and its database component against the target deployment before relying on it as a readiness gate.

## Concurrency Design

- PostgreSQL row locks serialize competing reservations for the same seats. Multi-seat seat names are sorted before locking to keep lock order deterministic.
- A guarded update changes only rows whose status is still `available`; the transaction rolls back if every requested seat is not updated.
- A PostgreSQL transaction-scoped advisory lock serializes requests by user before the per-user limit and idempotency checks.
- Multi-seat reservation is all-or-nothing. If any seat is unknown or unavailable, the transaction does not book a partial subset.
- The implementation currently books seats directly. `held` is recognized in state/count logic, but there is no hold creation or expiry workflow.

## Run the Hot-Seat Burst Test

The script requires k6 and a running API plus a test show containing the requested seats. Run it from its directory so the relative JS path resolves:

```bash
cd Testing/reserve
./run-test.sh
```

The script prompts for:

1. Show ID.
2. Base URL (default `http://localhost:8080`).
3. Number of virtual users (default `100`).
4. Seats, comma-separated (default `A2`; for a two-seat all-or-nothing contention test, enter `A1,A2`).

Every virtual user sends one request for the same seat list, using a distinct token-derived user ID and idempotency key. The expected result is one `201`, with the other requests returning `409`. Ensure all listed seats start available and `per_user_limit` is at least the number of requested seats. A successful run consumes those seats; cancel that reservation or prepare a fresh show before repeating the test.

The script currently groups every `409` as a seat conflict and groups all other non-`201` responses together as unexpected. It does not query the final show state, verify the reconciliation invariant, test same-key retries, or break declines down by reason. It is a focused hot-seat test, not the complete 20,000-request acceptance harness described in the scenario.

On Windows, run `Testing\reserve\run-test.bat`; it prompts for the same values. For a deployed API, enter its public base URL. A cloud test runner cannot reach your laptop's `localhost` unless you deliberately expose it through a secure tunnel; prefer a dedicated staging deployment.

## Metrics and Logs

The reservation controller increments Micrometer counters in-process for booked reservations and declines. The current dependencies/configuration do not provide a Prometheus scrape endpoint or a seat-availability gauge. Request-ID structured logging is also not configured. Do not treat `/actuator/prometheus` as available until a Prometheus registry and endpoint exposure are added and verified.

## Before Public Submission

- Rotate and remove any database credential committed in source/configuration, including from Git history if it was pushed. Use deployment secrets/environment variables.
- Deploy the service and replace the local base URL with the actual public URL.
- Add and verify readiness/liveness probes, Prometheus export plus the required counters/gauge, and request-correlated structured logs.
- Extend the burst harness to cover retries, per-user limits, response reason distributions, 5xx counts, and final seat reconciliation.
- Add automated correctness tests that exercise concurrent reservations against PostgreSQL.