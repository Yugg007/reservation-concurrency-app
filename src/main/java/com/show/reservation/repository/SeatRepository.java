package com.show.reservation.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.show.reservation.entity.Seat;
import com.show.reservation.entity.SeatId;

import jakarta.persistence.LockModeType;

public interface SeatRepository extends JpaRepository<Seat, SeatId> {

    // Row locks (SELECT ... FOR UPDATE), always in sorted seat order -> no deadlocks
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id.showId = :showId and s.id.seatNo in :seatNos " +
           "order by s.id.seatNo")
    List<Seat> lockSeats(@Param("showId") UUID showId, @Param("seatNos") Collection<String> seatNos);

    @Query("select s from Seat s where s.id.showId = :showId order by s.id.seatNo")
    List<Seat> findAllForShow(@Param("showId") UUID showId);

    @Query("select count(s) from Seat s where s.id.showId = :showId and s.userId = :userId " +
           "and s.status in ('held','booked')")
    long countOwned(@Param("showId") UUID showId, @Param("userId") String userId);

    // Guarded conditional update: only books seats that are STILL available
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Seat s set s.status = 'booked', s.userId = :userId, s.reservationId = :rid " +
           "where s.id.showId = :showId and s.id.seatNo in :seatNos and s.status = 'available'")
    int bookIfAvailable(@Param("showId") UUID showId, @Param("seatNos") Collection<String> seatNos,
                        @Param("userId") String userId, @Param("rid") UUID rid);

    // Guarded release: only frees seats still booked by THIS reservation
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Seat s set s.status = 'available', s.userId = null, s.reservationId = null " +
           "where s.reservationId = :rid and s.status = 'booked'")
    int release(@Param("rid") UUID rid);

	// Per-user serialization. Cast because Hibernate can't map Postgres' void
	// return type.
	@Query(value = "select cast(pg_advisory_xact_lock(hashtext(cast(:userId as text))) as varchar)", nativeQuery = true)
	String lockUser(@Param("userId") String userId);
}