package com.ktroute.app.controller;

import com.ktroute.app.dto.request.DriverRequest;
import com.ktroute.app.dto.response.ApiResponse;
import com.ktroute.app.dto.response.DriverResponse;
import com.ktroute.app.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/drivers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAllDrivers() {
        return ResponseEntity.ok(ApiResponse.success(driverService.getAllDrivers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriver(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(driverService.getDriver(id)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAvailableDrivers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(ApiResponse.success(driverService.getAvailableDrivers(start, end)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(@Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Driver created", driverService.createDriver(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(
            @PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Driver updated", driverService.updateDriver(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateDriver(@PathVariable Long id) {
        driverService.deactivateDriver(id);
        return ResponseEntity.ok(ApiResponse.success("Driver deactivated", null));
    }
}
