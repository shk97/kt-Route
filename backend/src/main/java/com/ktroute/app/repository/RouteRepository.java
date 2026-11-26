package com.ktroute.app.repository;

import com.ktroute.app.entity.Route;
import com.ktroute.app.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByRouteCode(String routeCode);
    boolean existsByRouteCode(String routeCode);
    long countByStatus(RouteStatus status);

    @Query("SELECT DISTINCT r FROM Route r LEFT JOIN FETCH r.startStation LEFT JOIN FETCH r.endStation")
    List<Route> findAllWithStations();

    @Query("SELECT DISTINCT r FROM Route r LEFT JOIN FETCH r.startStation LEFT JOIN FETCH r.endStation WHERE r.status = :status")
    List<Route> findByStatusWithStations(@Param("status") RouteStatus status);

    List<Route> findByStatus(RouteStatus status);

    @Query("""
        SELECT r,
               COUNT(DISTINCT b.id) as passengerCount,
               SUM(b.totalAmount) as revenue
        FROM Route r
        LEFT JOIN r.trips t
        LEFT JOIN t.bookings b ON b.status IN ('CONFIRMED', 'COMPLETED')
        WHERE (:from IS NULL OR t.departureTime >= :from)
          AND (:to IS NULL OR t.departureTime <= :to)
          AND r.status = 'ACTIVE'
        GROUP BY r
        ORDER BY passengerCount DESC
        """)
    List<Object[]> findRoutesByPassengerDemand(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    @Query("SELECT r FROM Route r WHERE r.startStation.id = :startId AND r.endStation.id = :endId AND r.status = 'ACTIVE'")
    List<Route> findByStartAndEnd(@Param("startId") Long startId, @Param("endId") Long endId);

    @Query("SELECT r FROM Route r JOIN r.stops s WHERE s.station.id = :stationId AND r.status = 'ACTIVE'")
    List<Route> findByStationId(@Param("stationId") Long stationId);
}
