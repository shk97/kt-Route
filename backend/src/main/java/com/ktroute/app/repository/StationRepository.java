package com.ktroute.app.repository;

import com.ktroute.app.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByCode(String code);
    boolean existsByCode(String code);
    List<Station> findByIsActiveTrue();
    List<Station> findByNameContainingIgnoreCase(String name);

    @Query("""
        SELECT s,
               COUNT(DISTINCT br.id) as boardingCount,
               COUNT(DISTINCT br2.id) as alightingCount
        FROM Station s
        LEFT JOIN BoardingRecord br ON br.boardingStation = s
        LEFT JOIN BoardingRecord br2 ON br2.alightingStation = s
        WHERE (:from IS NULL OR br.recordedAt >= :from)
          AND (:to IS NULL OR br.recordedAt <= :to)
        GROUP BY s
        ORDER BY boardingCount DESC
        """)
    List<Object[]> findStationActivityRanked(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
}
