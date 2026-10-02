package com.show.reservation.entity;

import java.util.UUID;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "seats")
public class Seat implements Persistable<SeatId> {
    @EmbeddedId private SeatId id;
    private String status;                                   // available | held | booked
    @Column(name = "user_id") private String userId;
    @Column(name = "reservation_id") private UUID reservationId;

    @Transient private boolean isNew = true;                 // avoids a SELECT per seat on insert

    protected Seat() {}
    public Seat(UUID showId, String seatNo) { this.id = new SeatId(showId, seatNo); this.status = "available"; }

    public SeatId getId() { return id; }
    public String getStatus() { return status; }
    public String getUserId() { return userId; }
    public UUID getReservationId() { return reservationId; }

    @Override public boolean isNew() { return isNew; }
    @PostLoad @PostPersist void markNotNew() { this.isNew = false; }
}