package com.ktroute.app.controller;

import com.ktroute.app.dto.response.*;
import com.ktroute.app.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDashboardStats()));
    }

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<RouteDemandResponse>>> getRouteDemand(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "passengers_desc") String sortBy) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getRouteDemand(f, t, sortBy)));
    }

    @GetMapping("/stations")
    public ResponseEntity<ApiResponse<List<DashboardStats.StationActivity>>> getStationActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getStationActivity(f, t)));
    }

    @GetMapping("/capacity")
    public ResponseEntity<ApiResponse<List<CapacityReport>>> getCapacity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusMonths(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now().plusMonths(1);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getCapacityReport(f, t)));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<DashboardStats.MonthlyDemand>>> getMonthly(
            @RequestParam(defaultValue = "2024") int fromYear,
            @RequestParam(defaultValue = "2026") int toYear) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getMonthlyDemand(fromYear, toYear)));
    }

    @GetMapping("/monthly/route/{routeId}")
    public ResponseEntity<ApiResponse<List<DashboardStats.MonthlyDemand>>> getMonthlyByRoute(
            @PathVariable Long routeId,
            @RequestParam(defaultValue = "2024") int fromYear,
            @RequestParam(defaultValue = "2026") int toYear) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getMonthlyDemandForRoute(routeId, fromYear, toYear)));
    }

    @GetMapping("/profitability")
    public ResponseEntity<ApiResponse<List<ProfitabilityResponse>>> getProfitability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getProfitability(f, t)));
    }

    // ── Station Boarding Analytics ────────────────────────────────────────────

    @GetMapping("/stations/overview")
    public ResponseEntity<ApiResponse<StationAnalyticsResponse.Overview>> getStationOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getStationOverview(f, t)));
    }

    @GetMapping("/stations/ranked")
    public ResponseEntity<ApiResponse<List<StationAnalyticsResponse.StationBoarding>>> getStationRanked(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getStationBoardingRanked(f, t)));
    }

    @GetMapping("/stations/{stationId}/routes")
    public ResponseEntity<ApiResponse<List<StationAnalyticsResponse.RouteBreakdown>>> getStationRoutes(
            @PathVariable Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getStationRouteBreakdown(stationId, f, t)));
    }

    @GetMapping("/stations/{stationId}/routes/{routeId}/trips")
    public ResponseEntity<ApiResponse<List<StationAnalyticsResponse.TripBoarding>>> getStationRouteTrips(
            @PathVariable Long stationId,
            @PathVariable Long routeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getStationTripBreakdown(stationId, routeId, f, t)));
    }

    @GetMapping("/stations/{stationId}/peak-hours")
    public ResponseEntity<ApiResponse<List<StationAnalyticsResponse.PeakHour>>> getStationPeakHours(
            @PathVariable Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getStationPeakHours(stationId, f, t)));
    }

    @GetMapping("/stations/trends")
    public ResponseEntity<ApiResponse<List<StationAnalyticsResponse.DailyTrend>>> getStationTrends(
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusMonths(3);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDailyTrend(stationId, f, t)));
    }

    @GetMapping("/stations/map")
    public ResponseEntity<ApiResponse<List<StationAnalyticsResponse.MapStation>>> getStationMap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getMapStations(f, t)));
    }

    @GetMapping("/stations/recommendations")
    public ResponseEntity<ApiResponse<List<StationAnalyticsResponse.Recommendation>>> getRecommendations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getRecommendations(f, t)));
    }
}

