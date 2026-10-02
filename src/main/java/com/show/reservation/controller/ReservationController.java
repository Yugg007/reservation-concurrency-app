package com.show.reservation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.MeterRegistry;

import com.show.reservation.dto.ReservationResponse;
import com.show.reservation.dto.ReserveRequest;
import com.show.reservation.service.ReservationService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ReservationController {
	private final ReservationService svc;
	private final MeterRegistry metrics;

	public ReservationController(ReservationService svc, MeterRegistry metrics) {
		this.svc = svc;
		this.metrics = metrics;
	}

	@PostMapping("/shows/{id}/reserve")
	public ResponseEntity<ReservationResponse> reserve(@PathVariable String id, @RequestBody ReserveRequest body,
			@RequestHeader(value = "Idempotency-Key", required = false) String headerKey, HttpServletRequest req) throws java.lang.Exception {
		String userId = (String) req.getAttribute("userId");
		String key = headerKey != null ? headerKey : body.idempotency_key();
		if (key == null || key.isBlank() || body.seats() == null || body.seats().isEmpty())
			throw new Exception("bad_request" + "seats and idempotency key required");
		try {
			var result = svc.reserve(ShowController.parse(id), userId, key, body.seats());
			if (result.replay()) {
				metrics.counter("reservations_declined_total", "reason", "idempotent_replay").increment();
				return ResponseEntity.ok(result.body()); // 200 for a replay
			}
			metrics.counter("reservations_confirmed_total").increment();
			return ResponseEntity.status(201).body(result.body());
		} catch (Exception e) {
			throw e;
		}
	}

	@PostMapping("/reservations/{id}/cancel")
	public ReservationResponse cancel(@PathVariable String id, HttpServletRequest req) throws java.lang.Exception {
		return svc.cancel(ShowController.parse(id), (String) req.getAttribute("userId"));
	}
}