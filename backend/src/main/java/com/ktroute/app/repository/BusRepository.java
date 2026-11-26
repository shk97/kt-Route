package com.ktroute.app.repository;

import com.ktroute.app.entity.Bus;
import com.ktroute.app.enums.BusStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus, Long> {
    Optional<Bus> findByBusNumber(String busNumber);
    boolean existsByBusNumber(String busNumber);
    boolean existsByRegistrationNo(String registrationNo);
    List<Bus> findByStatus(BusStatus status);
    long countByStatus(BusStatus status);

    @Query("""
        SELECT b FROM Bus b WHERE b.status = 'ACTIVE'
        AND b.id NOT IN (
            SELECT t.bus.id FROM Trip t
            WHERE t.status IN ('SCHEDULED','IN_PROGRESS')
            AND t.departureTime < :end
            AND t.arrivalTime > :start
        )
        """)
    List<Bus> findAvailableBuses(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);
}
