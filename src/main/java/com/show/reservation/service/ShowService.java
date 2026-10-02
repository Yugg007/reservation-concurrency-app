package com.show.reservation.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.show.reservation.common.ApiException;
import com.show.reservation.dto.CreateShowRequest;
import com.show.reservation.dto.ShowResponse;
import com.show.reservation.entity.Seat;
import com.show.reservation.entity.Show;
import com.show.reservation.repository.SeatRepository;
import com.show.reservation.repository.ShowRepository;

@Service
public class ShowService {
	private final ShowRepository showRepository;
	private final SeatRepository seatRepository;

	public ShowService(ShowRepository showRepository, SeatRepository seatRepository) {
		this.showRepository = showRepository;
		this.seatRepository = seatRepository;
	}

	@Transactional
	public ShowResponse create(CreateShowRequest r) {
		if (r.name() == null || r.name().isBlank() || r.seats() == null || r.seats().isEmpty()
				|| r.price_paise() == null || r.price_paise() <= 0
				|| r.seats().stream().anyMatch(s -> s == null || s.isBlank()))
			throw new ApiException(400, "bad_request", "name, seats, price_paise required");

		if (r.seats().stream().distinct().count() != r.seats().size())
			throw new ApiException(400, "bad_request", "duplicate seats");

		int limit = r.per_user_limit() == null ? 4 : r.per_user_limit();

		if (limit < 1)
			throw new ApiException(400, "bad_request", "per_user_limit must be >= 1");

		Show show = showRepository.save(new Show(r.name(), r.price_paise(), limit));
		UUID id = show.getId();
		seatRepository.saveAll(r.seats().stream().map(s -> new Seat(id, s)).toList());
		return get(id);
	}

	@Transactional(readOnly = true)
	public ShowResponse get(UUID id) {
		Show show = showRepository.findById(id).orElseThrow(() -> new ApiException(404, "not_found", "show not found"));
		Map<String, String> map = new LinkedHashMap<>();
		int a = 0, h = 0, c = 0;
		for (Seat s : seatRepository.findAllForShow(id)) { // one snapshot -> invariant is exact
			map.put(s.getId().seatNo(), s.getStatus());
			switch (s.getStatus()) {
				case "available" -> a++;
				case "held" -> h++;
				default -> c++;
			}
		}
		return new ShowResponse(id.toString(), show.getName(), show.getPricePaise(), show.getPerUserLimit(), map.size(),
				a, h, c, map);
	}
}