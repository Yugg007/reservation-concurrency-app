package com.show.reservation.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record SeatId(
        @Column(name = "show_id") UUID showId,
        @Column(name = "seat_no") String seatNo) implements Serializable {}