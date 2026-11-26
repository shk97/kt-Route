package com.ktroute.app.repository;

import com.ktroute.app.entity.RouteOperatingCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RouteOperatingCostRepository extends JpaRepository<RouteOperatingCost, Long> {
    Optional<RouteOperatingCost> findByRouteIdAndMonthAndYear(Long routeId, int month, int year);
    List<RouteOperatingCost> findByRouteId(Long routeId);

    @Query("""
        SELECT roc.route.id,
               SUM(roc.fuelCost + roc.driverCost + roc.maintenanceCost + roc.tollCost + roc.otherCosts)
        FROM RouteOperatingCost roc
        WHERE roc.year = :year AND roc.month = :month
        GROUP BY roc.route.id
        """)
    List<Object[]> findMonthlyCostByRoute(@Param("year") int year, @Param("month") int month);

    @Query("""
        SELECT roc.month, roc.year,
               SUM(roc.fuelCost + roc.driverCost + roc.maintenanceCost + roc.tollCost + roc.otherCosts)
        FROM RouteOperatingCost roc
        WHERE roc.route.id = :routeId
          AND roc.year BETWEEN :fromYear AND :toYear
        GROUP BY roc.year, roc.month
        ORDER BY roc.year, roc.month
        """)
    List<Object[]> findCostTrendForRoute(@Param("routeId") Long routeId,
                                          @Param("fromYear") int fromYear,
                                          @Param("toYear") int toYear);

    @Query("""
        SELECT roc.route.id,
               SUM(roc.fuelCost + roc.driverCost + roc.maintenanceCost + roc.tollCost + roc.otherCosts)
        FROM RouteOperatingCost roc
        WHERE (roc.year > :fromYear OR (roc.year = :fromYear AND roc.month >= :fromMonth))
          AND (roc.year < :toYear OR (roc.year = :toYear AND roc.month <= :toMonth))
        GROUP BY roc.route.id
        """)
    List<Object[]> sumCostByRouteInRange(@Param("fromYear") int fromYear, @Param("fromMonth") int fromMonth,
                                          @Param("toYear") int toYear, @Param("toMonth") int toMonth);
}
