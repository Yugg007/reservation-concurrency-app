import http from 'k6/http';
import { Counter } from 'k6/metrics';

const vus = Number(__ENV.VUS || 100);
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const showId = __ENV.SHOW_ID;
const seat = __ENV.SEAT || 'A2';

if (!showId) {
  throw new Error(
    'SHOW_ID is required. Please provide an existing show containing the test seat.'
  );
}

const created = new Counter('reservations_created');
const conflicts = new Counter('seat_conflicts');
const unexpected = new Counter('unexpected_responses');

export const options = {
  scenarios: {
    contention: {
      executor: 'per-vu-iterations',
      vus: vus,
      iterations: 1,
      maxDuration: '10m',
    },
  },

  thresholds: {
    reservations_created: [`count == 1`],
    seat_conflicts: [`count == ${vus - 1}`],
    unexpected_responses: ['count == 0'],
  },
};

export default function () {
  /*
   * Every virtual user represents a different user.
   *
   * Therefore:
   *   User 1  -> A2
   *   User 2  -> A2
   *   User 3  -> A2
   *   ...
   *
   * Only ONE request should successfully reserve A2.
   */

  const key = `load-${__VU}-${__ITER}`;

  const response = http.post(
    `${baseUrl}/shows/${showId}/reserve`,
    JSON.stringify({
      idempotency_key: key,
      seats: [seat],
    }),
    {
      headers: {
        Authorization: `Bearer load-user-${__VU}`,
        'Content-Type': 'application/json',
        'Idempotency-Key': key,
      },
    }
  );

  if (response.status === 201) {
    created.add(1);
  } else if (response.status === 409) {
    conflicts.add(1);
  } else {
    unexpected.add(1);

    console.log(
      `Unexpected response: status=${response.status}, body=${response.body}`
    );
  }
}

export function handleSummary(data) {
  const successful =
    data.metrics.reservations_created?.values?.count || 0;

  const conflictsCount =
    data.metrics.seat_conflicts?.values?.count || 0;

  const unexpectedCount =
    data.metrics.unexpected_responses?.values?.count || 0;

  const total = successful + conflictsCount + unexpectedCount;

  const passed =
    successful === 1 &&
    conflictsCount === vus - 1 &&
    unexpectedCount === 0;

  const result = passed ? 'PASS' : 'FAIL';

  return {
    stdout: `
============================================================
              BOOKING CONCURRENCY TEST
============================================================

Base URL       : ${baseUrl}
Show ID        : ${showId}
Seat           : ${seat}
Concurrent Users: ${vus}

------------------------------------------------------------
RESULT
------------------------------------------------------------

Total Requests       : ${total}
Successful Bookings  : ${successful}
Seat Conflicts       : ${conflictsCount}
Unexpected Responses : ${unexpectedCount}

------------------------------------------------------------

Expected:
  Successful Bookings  = 1
  Seat Conflicts       = ${vus - 1}
  Unexpected Responses = 0

------------------------------------------------------------

                    ${result}
------------------------------------------------------------
`,
  };
}