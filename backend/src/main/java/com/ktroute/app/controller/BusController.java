package com.ktroute.app.controller;

import com.ktroute.app.dto.request.BusRequest;
import com.ktroute.app.dto.response.ApiResponse;
import com.ktroute.app.dto.response.BusResponse;
import com.ktroute.app.service.BusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/buses")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BusController {

    private final BusService busService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusResponse>>> getAllBuses() {
        return ResponseEntity.ok(ApiResponse.success(busService.getAllBuses()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusResponse>> getBus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(busService.getBus(id)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<BusResponse>>> getAvailableBuses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(ApiResponse.success(busService.getAvailableBuses(start, end)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusResponse>> createBus(@Valid @RequestBody BusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bus created", busService.createBus(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusResponse>> updateBus(
            @PathVariable Long id, @Valid @RequestBody BusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bus updated", busService.updateBus(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateBus(@PathVariable Long id) {
        busService.deactivateBus(id);
        return ResponseEntity.ok(ApiResponse.success("Bus deactivated", null));
    }
}
