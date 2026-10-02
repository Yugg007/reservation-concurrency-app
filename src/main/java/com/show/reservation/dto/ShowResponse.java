package com.show.reservation.dto;

import java.util.Map;

public record ShowResponse(
	String id,
	String name,
	long price_paise,
	int per_user_limit,
	int total_seats,
	int available_seats,
	int held_seats,
	int confirmed_seats,
	Map<String, String> seats) {
}
