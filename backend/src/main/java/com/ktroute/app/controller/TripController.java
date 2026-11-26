package com.ktroute.app.controller;

import com.ktroute.app.dto.request.TripRequest;
import com.ktroute.app.dto.response.ApiResponse;
import com.ktroute.app.dto.response.TripResponse;
import com.ktroute.app.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    // Public - search trips
    @GetMapping("/api/trips/search")
    public ResponseEntity<ApiResponse<List<TripResponse>>> searchTrips(
            @RequestParam Long startStationId,
            @RequestParam Long endStationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(tripService.searchTrips(startStationId, endStationId, date)));
    }

    @GetMapping("/api/trips/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tripService.getTrip(id)));
    }

    // Admin endpoints
    @GetMapping("/api/admin/trips")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getAllTrips() {
        return ResponseEntity.ok(ApiResponse.success(tripService.getAllTrips()));
    }

    @GetMapping("/api/admin/trips/route/{routeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripsByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(ApiResponse.success(tripService.getTripsByRoute(routeId)));
    }

    @PostMapping("/api/admin/trips")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody TripRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Trip created",
                tripService.createTrip(request, userDetails.getUsername())));
    }

    @PutMapping("/api/admin/trips/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody TripRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Trip updated",
                tripService.updateTrip(id, request, userDetails.getUsername())));
    }

    @PostMapping("/api/admin/trips/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelTrip(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        tripService.cancelTrip(id, body.get("reason"), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Trip cancelled", null));
    }
}
