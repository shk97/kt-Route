package com.ktroute.app.controller;

import com.ktroute.app.dto.request.RouteRequest;
import com.ktroute.app.dto.response.ApiResponse;
import com.ktroute.app.dto.response.RouteResponse;
import com.ktroute.app.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping("/api/routes")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getAllRoutes() {
        return ResponseEntity.ok(ApiResponse.success(routeService.getActiveRoutes()));
    }

    @GetMapping("/api/routes/{id}")
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(routeService.getRoute(id)));
    }

    @PostMapping("/api/admin/routes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Route created", routeService.createRoute(request)));
    }

    @PutMapping("/api/admin/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(
            @PathVariable Long id, @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Route updated", routeService.updateRoute(id, request)));
    }

    @DeleteMapping("/api/admin/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateRoute(@PathVariable Long id) {
        routeService.deactivateRoute(id);
        return ResponseEntity.ok(ApiResponse.success("Route deactivated", null));
    }

    @GetMapping("/api/admin/routes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getAllRoutesAdmin() {
        return ResponseEntity.ok(ApiResponse.success(routeService.getAllRoutes()));
    }
}
