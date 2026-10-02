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
@Table(name = "shows")
public class Show {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    @Column(name = "price_paise") private long pricePaise;
    @Column(name = "per_user_limit") private int perUserLimit;
    @Column(name = "created_at", insertable = false, updatable = false) private Instant createdAt;

    protected Show() {}
    public Show(String name, long pricePaise, int perUserLimit) {
        this.name = name; this.pricePaise = pricePaise; this.perUserLimit = perUserLimit;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public long getPricePaise() { return pricePaise; }
    public int getPerUserLimit() { return perUserLimit; }
}