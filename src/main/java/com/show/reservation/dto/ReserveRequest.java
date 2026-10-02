package com.show.reservation.dto;

import java.util.List;

public record ReserveRequest(String idempotency_key, List<String> seats) {
}
