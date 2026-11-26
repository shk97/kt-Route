package com.ktroute.app.repository;

import com.ktroute.app.entity.Booking;
import com.ktroute.app.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingRef(String bookingRef);
    List<Booking> findByUserId(Long userId);
    List<Booking> findByTripId(Long tripId);
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByStatus(BookingStatus status);
    long countByStatusIn(List<BookingStatus> statuses);

    @Query("""
        SELECT COALESCE(SUM(b.passengerCount), 0)
        FROM Booking b
        WHERE b.trip.id = :tripId
          AND b.status IN ('CONFIRMED','COMPLETED')
        """)
    Integer countConfirmedPassengersForTrip(@Param("tripId") Long tripId);

    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0)
        FROM Booking b
        WHERE b.status IN ('CONFIRMED','COMPLETED')
          AND b.trip.departureTime >= :from
          AND b.trip.departureTime <= :to
        """)
    java.math.BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query("""
        SELECT COUNT(DISTINCT b.id)
        FROM Booking b
        WHERE b.status IN ('CONFIRMED','COMPLETED')
          AND b.trip.departureTime >= :from
          AND b.trip.departureTime <= :to
        """)
    long countCompletedBookingsBetween(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("""
        SELECT MONTH(b.trip.departureTime), YEAR(b.trip.departureTime),
               COUNT(b.id), SUM(b.passengerCount), SUM(b.totalAmount)
        FROM Booking b
        WHERE b.status IN ('CONFIRMED','COMPLETED')
          AND b.trip.departureTime >= :from
          AND b.trip.departureTime <= :to
        GROUP BY YEAR(b.trip.departureTime), MONTH(b.trip.departureTime)
        ORDER BY YEAR(b.trip.departureTime), MONTH(b.trip.departureTime)
        """)
    List<Object[]> findMonthlyDemand(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("""
        SELECT b.trip.route.id, b.trip.route.name,
               COUNT(DISTINCT b.id), SUM(b.passengerCount), SUM(b.totalAmount)
        FROM Booking b
        WHERE b.status IN ('CONFIRMED','COMPLETED')
          AND b.trip.departureTime >= :from
          AND b.trip.departureTime <= :to
        GROUP BY b.trip.route.id, b.trip.route.name
        ORDER BY SUM(b.passengerCount) DESC
        """)
    List<Object[]> findRouteWiseDemand(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.trip.id = :tripId
          AND b.status IN ('CONFIRMED','COMPLETED')
        """)
    List<Booking> findActiveBookingsForTrip(@Param("tripId") Long tripId);

    @Query("""
        SELECT b.trip.id, COALESCE(SUM(b.passengerCount), 0)
        FROM Booking b
        WHERE b.trip.id IN :tripIds
          AND b.status IN ('CONFIRMED','COMPLETED')
        GROUP BY b.trip.id
        """)
    List<Object[]> countConfirmedPassengersForTripIds(@Param("tripIds") List<Long> tripIds);

    @Query("""
        SELECT MONTH(b.trip.departureTime), YEAR(b.trip.departureTime),
               COUNT(b.id), SUM(b.passengerCount), SUM(b.totalAmount)
        FROM Booking b
        WHERE b.status IN ('CONFIRMED','COMPLETED')
          AND b.trip.route.id = :routeId
          AND b.trip.departureTime >= :from
          AND b.trip.departureTime <= :to
        GROUP BY YEAR(b.trip.departureTime), MONTH(b.trip.departureTime)
        ORDER BY YEAR(b.trip.departureTime), MONTH(b.trip.departureTime)
        """)
    List<Object[]> findMonthlyDemandByRoute(@Param("routeId") Long routeId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    boolean existsByTripIdAndUserIdAndStatusIn(Long tripId, Long userId, List<BookingStatus> statuses);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.route
        LEFT JOIN FETCH t.bus
        LEFT JOIN FETCH b.boardingStation
        LEFT JOIN FETCH b.alightingStation
        LEFT JOIN FETCH b.passengers
        WHERE b.id = :id
        """)
    Optional<Booking> findByIdWithAllAssociations(@Param("id") Long id);
}
