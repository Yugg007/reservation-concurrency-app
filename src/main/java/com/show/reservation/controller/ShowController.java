package com.show.reservation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.show.reservation.dto.CreateShowRequest;
import com.show.reservation.dto.ShowResponse;
import com.show.reservation.service.ShowService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ShowController {
	private final ShowService shows;

	public ShowController(ShowService shows) {
		this.shows = shows;
	}

	@PostMapping("/shows")
	public ResponseEntity<ShowResponse> create(@RequestBody CreateShowRequest body, HttpServletRequest req) throws Exception {
		if (!Boolean.TRUE.equals(req.getAttribute("admin")))
			throw new Exception("forbidden, admin only");
		return ResponseEntity.status(201).body(shows.create(body));
	}

	@GetMapping("/shows/{id}")
	public ShowResponse get(@PathVariable String id) throws Exception {
		return shows.get(parse(id));
	}

	static UUID parse(String s) throws Exception {
		try {
			return UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			throw new Exception("not_found, invalid id");
		}
	}
}
