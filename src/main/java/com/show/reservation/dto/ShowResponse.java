package com.show.reservation.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ShowResponse(
	String showId,
	String name,
	long price_paise,
	int per_user_limit,
	int total_seats,
	int available_seats,
	int held_seats,
	int booked_seats,
	Map<String, SeatDetails> seats) {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SeatDetails(String status, String booked_by, String reservationId) {}
}
