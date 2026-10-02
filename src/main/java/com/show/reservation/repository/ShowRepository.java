package com.show.reservation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.show.reservation.entity.Show;

public interface ShowRepository extends JpaRepository<Show, UUID> {}