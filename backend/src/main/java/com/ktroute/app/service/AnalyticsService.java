package com.ktroute.app.service;

import com.ktroute.app.dto.response.*;
import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsService {
    DashboardStats getDashboardStats();
    List<RouteDemandResponse> getRouteDemand(LocalDateTime from, LocalDateTime to, String sortBy);
    List<DashboardStats.StationActivity> getStationActivity(LocalDateTime from, LocalDateTime to);
    List<CapacityReport> getCapacityReport(LocalDateTime from, LocalDateTime to);
    List<DashboardStats.MonthlyDemand> getMonthlyDemand(int fromYear, int toYear);
    List<DashboardStats.MonthlyDemand> getMonthlyDemandForRoute(Long routeId, int fromYear, int toYear);
    List<ProfitabilityResponse> getProfitability(LocalDateTime from, LocalDateTime to);

    // Station boarding analytics
    StationAnalyticsResponse.Overview getStationOverview(LocalDateTime from, LocalDateTime to);
    List<StationAnalyticsResponse.StationBoarding> getStationBoardingRanked(LocalDateTime from, LocalDateTime to);
    List<StationAnalyticsResponse.RouteBreakdown> getStationRouteBreakdown(Long stationId, LocalDateTime from, LocalDateTime to);
    List<StationAnalyticsResponse.TripBoarding> getStationTripBreakdown(Long stationId, Long routeId, LocalDateTime from, LocalDateTime to);
    List<StationAnalyticsResponse.PeakHour> getStationPeakHours(Long stationId, LocalDateTime from, LocalDateTime to);
    List<StationAnalyticsResponse.DailyTrend> getDailyTrend(Long stationId, LocalDateTime from, LocalDateTime to);
    List<StationAnalyticsResponse.MapStation> getMapStations(LocalDateTime from, LocalDateTime to);
    List<StationAnalyticsResponse.Recommendation> getRecommendations(LocalDateTime from, LocalDateTime to);
}

