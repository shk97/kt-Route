package com.ktroute.app.controller;

import com.ktroute.app.dto.request.StationRequest;
import com.ktroute.app.dto.response.ApiResponse;
import com.ktroute.app.dto.response.StationResponse;
import com.ktroute.app.service.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping("/api/stations")
    public ResponseEntity<ApiResponse<List<StationResponse>>> getAllStations() {
        return ResponseEntity.ok(ApiResponse.success(stationService.getActiveStations()));
    }

    @GetMapping("/api/stations/{id}")
    public ResponseEntity<ApiResponse<StationResponse>> getStation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(stationService.getStation(id)));
    }

    @PostMapping("/api/admin/stations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StationResponse>> createStation(@Valid @RequestBody StationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Station created", stationService.createStation(request)));
    }

    @PutMapping("/api/admin/stations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StationResponse>> updateStation(
            @PathVariable Long id, @Valid @RequestBody StationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Station updated", stationService.updateStation(id, request)));
    }

    @DeleteMapping("/api/admin/stations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateStation(@PathVariable Long id) {
        stationService.deactivateStation(id);
        return ResponseEntity.ok(ApiResponse.success("Station deactivated", null));
    }
}
