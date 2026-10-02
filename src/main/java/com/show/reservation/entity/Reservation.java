package com.show.reservation.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "show_id") private UUID showId;
    @Column(name = "user_id") private String userId;
    @Column(name = "idempotency_key") private String idempotencyKey;
    @Column(name = "request_hash") private String requestHash;
    private String seats;                                    // sorted, comma separated
    @Column(name = "amount_paise") private long amountPaise;
    private String status;                                   // booked | cancelled
    @Column(name = "created_at", insertable = false, updatable = false) private Instant createdAt;

    protected Reservation() {}
    public Reservation(UUID showId, String userId, String key, String hash, String seats, long amount) {
        this.showId = showId; this.userId = userId; this.idempotencyKey = key;
        this.requestHash = hash; this.seats = seats; this.amountPaise = amount; this.status = "booked";
    }
    public UUID getId() { return id; }
    public UUID getShowId() { return showId; }
    public String getUserId() { return userId; }
    public String getRequestHash() { return requestHash; }
    public String getSeats() { return seats; }
    public long getAmountPaise() { return amountPaise; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}