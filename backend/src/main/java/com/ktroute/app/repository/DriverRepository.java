package com.ktroute.app.repository;

import com.ktroute.app.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    boolean existsByLicenseNo(String licenseNo);
    List<Driver> findByIsActiveTrue();

    @Query("""
        SELECT d FROM Driver d WHERE d.isActive = true
        AND d.id NOT IN (
            SELECT t.driver.id FROM Trip t
            WHERE t.driver IS NOT NULL
            AND t.status IN ('SCHEDULED','IN_PROGRESS')
            AND t.departureTime < :end
            AND t.arrivalTime > :start
        )
        """)
    List<Driver> findAvailableDrivers(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}
