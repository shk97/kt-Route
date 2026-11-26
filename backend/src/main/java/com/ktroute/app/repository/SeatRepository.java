package com.ktroute.app.repository;

import com.ktroute.app.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByBusIdAndIsActiveTrue(Long busId);

    @Query("""
        SELECT s FROM Seat s
        WHERE s.bus.id = :busId
          AND s.isActive = true
          AND s.seatNumber NOT IN (
              SELECT bp.seatNumber FROM BookingPassenger bp
              WHERE bp.booking.trip.id = :tripId
              AND bp.booking.status IN ('CONFIRMED','PENDING')
          )
        """)
    List<Seat> findAvailableSeats(@Param("busId") Long busId, @Param("tripId") Long tripId);
}
