package com.show.reservation.dto;

import java.util.List;

public record ReservationResponse(
	String id,
	String show_id,
	String user_id,
	List<String> seats,
	long amount_paise,
	String status) {
}
