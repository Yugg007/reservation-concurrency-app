package com.show.reservation.controller;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.show.reservation.common.ApiException;
import com.show.reservation.common.AppUtil;
import com.show.reservation.dto.CreateShowRequest;
import com.show.reservation.dto.ShowResponse;
import com.show.reservation.service.ShowService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ShowController {
	private final ShowService showService;

	public ShowController(ShowService showService) {
		this.showService = showService;
	}

	@PostMapping("/create/show")
	public ResponseEntity<ShowResponse> create(@RequestBody CreateShowRequest body, HttpServletRequest req) {
		if (!Boolean.TRUE.equals(req.getAttribute("admin")))
			throw new ApiException(403, "forbidden", "admin only");
		return ResponseEntity.status(201).body(showService.create(body));
	}

	@GetMapping("/show/{id}")
	public ShowResponse get(@PathVariable String id, HttpServletRequest req) {
		String userId = (String) req.getAttribute("userId");
		Boolean isAdmin =  Boolean.TRUE.equals(req.getAttribute("admin"));
		return showService.get(AppUtil.parse(id), userId, isAdmin);
	}
	
	@GetMapping("/shows")
	public List<ShowResponse> getAllShows(HttpServletRequest req) {
		String userId = (String) req.getAttribute("userId");
		Boolean isAdmin =  Boolean.TRUE.equals(req.getAttribute("admin"));
		return showService.getAllShows(userId, isAdmin);
	}

}
