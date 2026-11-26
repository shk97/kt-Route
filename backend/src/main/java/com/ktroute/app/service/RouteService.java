package com.ktroute.app.service;

import com.ktroute.app.dto.request.RouteRequest;
import com.ktroute.app.dto.response.RouteResponse;
import java.util.List;

public interface RouteService {
    RouteResponse createRoute(RouteRequest request);
    RouteResponse updateRoute(Long id, RouteRequest request);
    RouteResponse getRoute(Long id);
    List<RouteResponse> getAllRoutes();
    List<RouteResponse> getActiveRoutes();
    void deactivateRoute(Long id);
}
