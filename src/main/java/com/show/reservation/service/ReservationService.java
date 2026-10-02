package com.show.reservation.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.show.reservation.dto.ReservationResponse;
import com.show.reservation.entity.Reservation;
import com.show.reservation.entity.Seat;
import com.show.reservation.entity.Show;
import com.show.reservation.repository.ReservationRepository;
import com.show.reservation.repository.SeatRepository;
import com.show.reservation.repository.ShowRepository;

import jakarta.transaction.Transactional;


@Service
public class ReservationService {
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(ShowRepository showRepository, SeatRepository seatRepository, ReservationRepository reservationRepository) {
        this.showRepository = showRepository; this.seatRepository = seatRepository; this.reservationRepository = reservationRepository;
    }

    public record Result(ReservationResponse body, boolean replay) {}

    @Transactional
    public Result reserve(UUID showId, String userId, String key, List<String> rawSeats) throws Exception {
        List<String> wanted = rawSeats.stream().distinct().sorted().toList();   // deterministic lock order
        String seatsCsv = String.join(",", wanted);
        String hash = showId + "|" + seatsCsv;

        // (1) Serialize this user's requests -> limit check and idempotency are race-free
        seatRepository.lockUser(userId);

        // (2) Idempotency
        var existing = reservationRepository.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            Reservation e = existing.get();
            if (!hash.equals(e.getRequestHash()))
                throw new Exception("idempotency_mismatch key reused with a different request");
            return new Result(toResponse(e), true);
        }

        // (3) Show lookup
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new Exception("not_found show not found"));

        // (4) Per-user limit
        long owned = seatRepository.countOwned(showId, userId);
        if (owned + wanted.size() > show.getPerUserLimit())
            throw new Exception("per_user_limit, limit of " + show.getPerUserLimit() + " seats per show");

        // (5) Lock seat rows in sorted order, all-or-nothing
        List<Seat> locked = seatRepository.lockSeats(showId, wanted);
        if (locked.size() != wanted.size())
            throw new Exception("unknown_seat, one or more seats do not exist");
        for (Seat s : locked)
            if (!"available".equals(s.getStatus()))
                throw new Exception("seat_taken, seat " + s.getId().seatNo() + " already taken");

        // (6) Write: reservation row, then guarded update
        long amount = show.getPricePaise() * wanted.size();
        Reservation saved = reservationRepository.saveAndFlush(
                new Reservation(showId, userId, key, hash, seatsCsv, amount));
        int updated = seatRepository.confirmIfAvailable(showId, wanted, userId, saved.getId());
        if (updated != wanted.size())            // impossible after row locks; fail safe, rolls back
            throw new Exception("seat_taken, seat taken");

        return new Result(new ReservationResponse(saved.getId().toString(), showId.toString(),
                userId, wanted, amount, "confirmed"), false);
    }

    @Transactional
    public ReservationResponse cancel(UUID resId, String userId) throws Exception {
        Reservation r = reservationRepository.findForUpdate(resId).orElse(null);
        // not found OR not yours -> same 404 (don't leak existence)
        if (r == null || !userId.equals(r.getUserId()))
            throw new Exception("not_found, reservation not found");

        if ("confirmed".equals(r.getStatus())) {
            // Order matters: release() flushes pending changes, then clears the persistence context.
            r.setStatus("cancelled");
            seatRepository.release(resId);
        }
        return new ReservationResponse(resId.toString(), r.getShowId().toString(), userId,
                List.of(r.getSeats().split(",")), r.getAmountPaise(), "cancelled");
    }

    private ReservationResponse toResponse(Reservation e) {
        return new ReservationResponse(e.getId().toString(), e.getShowId().toString(), e.getUserId(),
                List.of(e.getSeats().split(",")), e.getAmountPaise(), e.getStatus());
    }
}