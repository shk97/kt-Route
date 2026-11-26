package com.ktroute.app.repository;

import com.ktroute.app.entity.ScheduleChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduleChangeHistoryRepository extends JpaRepository<ScheduleChangeHistory, Long> {
    List<ScheduleChangeHistory> findByTripIdOrderByChangedAtDesc(Long tripId);
}
