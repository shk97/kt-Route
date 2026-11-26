package com.ktroute.app.repository;

import com.ktroute.app.entity.BoardingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BoardingRecordRepository extends JpaRepository<BoardingRecord, Long> {
    List<BoardingRecord> findByTripId(Long tripId);
    List<BoardingRecord> findByBookingId(Long bookingId);

    @Query("""
        SELECT br.boardingStation.id,
               br.boardingStation.name,
               SUM(br.passengerCount) as boardingCount
        FROM BoardingRecord br
        WHERE (:from IS NULL OR br.recordedAt >= :from)
          AND (:to IS NULL OR br.recordedAt <= :to)
        GROUP BY br.boardingStation.id, br.boardingStation.name
        ORDER BY boardingCount DESC
        """)
    List<Object[]> findBoardingStatsByStation(@Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    @Query("""
        SELECT br.alightingStation.id,
               br.alightingStation.name,
               SUM(br.passengerCount) as alightingCount
        FROM BoardingRecord br
        WHERE (:from IS NULL OR br.recordedAt >= :from)
          AND (:to IS NULL OR br.recordedAt <= :to)
        GROUP BY br.alightingStation.id, br.alightingStation.name
        ORDER BY alightingCount DESC
        """)
    List<Object[]> findAlightingStatsByStation(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    /** Per-station KPI: total boarding, route count, trip count */
    @Query("""
        SELECT br.boardingStation.id,
               br.boardingStation.name,
               br.boardingStation.city,
               br.boardingStation.state,
               br.boardingStation.latitude,
               br.boardingStation.longitude,
               SUM(br.passengerCount)             AS totalBoarding,
               COUNT(DISTINCT br.trip.route.id)   AS routeCount,
               COUNT(DISTINCT br.trip.id)          AS tripCount
        FROM BoardingRecord br
        WHERE (:from IS NULL OR br.recordedAt >= :from)
          AND (:to   IS NULL OR br.recordedAt <= :to)
        GROUP BY br.boardingStation.id, br.boardingStation.name,
                 br.boardingStation.city, br.boardingStation.state,
                 br.boardingStation.latitude, br.boardingStation.longitude
        ORDER BY totalBoarding DESC
        """)
    List<Object[]> findStationBoardingRanked(@Param("from") LocalDateTime from,
                                              @Param("to")   LocalDateTime to);

    /** Route-wise breakdown for a single station */
    @Query("""
        SELECT br.trip.route.id,
               br.trip.route.name,
               br.trip.route.startStation.name,
               br.trip.route.endStation.name,
               SUM(br.passengerCount)         AS passengers,
               COUNT(DISTINCT br.trip.id)     AS tripCount
        FROM BoardingRecord br
        WHERE br.boardingStation.id = :stationId
          AND (:from IS NULL OR br.recordedAt >= :from)
          AND (:to   IS NULL OR br.recordedAt <= :to)
        GROUP BY br.trip.route.id, br.trip.route.name,
                 br.trip.route.startStation.name, br.trip.route.endStation.name
        ORDER BY passengers DESC
        """)
    List<Object[]> findRouteBreakdownForStation(@Param("stationId") Long stationId,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to")   LocalDateTime to);

    /** Trip-level details for a station + route combination */
    @Query("""
        SELECT br.trip.id,
               br.trip.bus.busNumber,
               br.trip.departureTime,
               SUM(br.passengerCount) AS passengers
        FROM BoardingRecord br
        WHERE br.boardingStation.id = :stationId
          AND br.trip.route.id = :routeId
          AND (:from IS NULL OR br.recordedAt >= :from)
          AND (:to   IS NULL OR br.recordedAt <= :to)
        GROUP BY br.trip.id, br.trip.bus.busNumber, br.trip.departureTime
        ORDER BY br.trip.departureTime DESC
        """)
    List<Object[]> findTripBreakdownForStationRoute(@Param("stationId") Long stationId,
                                                     @Param("routeId")   Long routeId,
                                                     @Param("from") LocalDateTime from,
                                                     @Param("to")   LocalDateTime to);

    /** Peak-hour breakdown for a station (hour-of-day from actual or recorded time) */
    @Query(value = """
        SELECT HOUR(COALESCE(actual_boarding_time, recorded_at)) AS hr,
               SUM(passenger_count) AS passengers
        FROM boarding_records
        WHERE boarding_station_id = :stationId
          AND (:from IS NULL OR recorded_at >= :from)
          AND (:to   IS NULL OR recorded_at <= :to)
        GROUP BY hr
        ORDER BY hr ASC
        """, nativeQuery = true)
    List<Object[]> findPeakHoursForStation(@Param("stationId") Long stationId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to")   LocalDateTime to);

    /** Daily boarding trend (all stations when stationId is null) */
    @Query(value = """
        SELECT DATE(recorded_at) AS day, SUM(passenger_count) AS passengers
        FROM boarding_records
        WHERE (:stationId IS NULL OR boarding_station_id = :stationId)
          AND (:from IS NULL OR recorded_at >= :from)
          AND (:to   IS NULL OR recorded_at <= :to)
        GROUP BY DATE(recorded_at)
        ORDER BY day ASC
        """, nativeQuery = true)
    List<Object[]> findDailyTrend(@Param("stationId") Long stationId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to")   LocalDateTime to);

    /** Total boarding passengers across all stations in a period */
    @Query("""
        SELECT SUM(br.passengerCount)
        FROM BoardingRecord br
        WHERE (:from IS NULL OR br.recordedAt >= :from)
          AND (:to   IS NULL OR br.recordedAt <= :to)
        """)
    Long sumTotalBoarding(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
