package com.ktroute.app.service.impl;

import com.ktroute.app.dto.request.RouteRequest;
import com.ktroute.app.dto.response.RouteResponse;
import com.ktroute.app.entity.Route;
import com.ktroute.app.entity.RouteStop;
import com.ktroute.app.entity.Station;
import com.ktroute.app.enums.RouteStatus;
import com.ktroute.app.exception.DuplicateResourceException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.RouteRepository;
import com.ktroute.app.repository.RouteStopRepository;
import com.ktroute.app.repository.StationRepository;
import com.ktroute.app.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final RouteStopRepository routeStopRepository;

    @Override
    @Transactional
    public RouteResponse createRoute(RouteRequest req) {
        if (routeRepository.existsByRouteCode(req.getRouteCode().toUpperCase())) {
            throw new DuplicateResourceException("Route code already exists: " + req.getRouteCode());
        }
        Station start = stationRepository.findById(req.getStartStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station", "id", req.getStartStationId()));
        Station end = stationRepository.findById(req.getEndStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station", "id", req.getEndStationId()));

        Route route = Route.builder()
                .name(req.getName())
                .routeCode(req.getRouteCode().toUpperCase())
                .startStation(start)
                .endStation(end)
                .distanceKm(req.getDistanceKm())
                .estimatedDurationMinutes(req.getEstimatedDurationMinutes())
                .status(req.getStatus() != null ? req.getStatus() : RouteStatus.ACTIVE)
                .description(req.getDescription())
                .build();
        route = routeRepository.save(route);

        if (req.getStops() != null) {
            final Route savedRoute = route;
            List<RouteStop> stops = new ArrayList<>();
            for (RouteRequest.RouteStopRequest sr : req.getStops()) {
                Station stopStation = stationRepository.findById(sr.getStationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Station", "id", sr.getStationId()));
                RouteStop stop = RouteStop.builder()
                        .route(savedRoute)
                        .station(stopStation)
                        .stopOrder(sr.getStopOrder())
                        .arrivalOffsetMinutes(sr.getArrivalOffsetMinutes() != null ? sr.getArrivalOffsetMinutes() : 0)
                        .departureOffsetMinutes(sr.getDepartureOffsetMinutes() != null ? sr.getDepartureOffsetMinutes() : 0)
                        .distanceFromStartKm(sr.getDistanceFromStartKm())
                        .build();
                stops.add(stop);
            }
            routeStopRepository.saveAll(stops);
        }
        return getRoute(route.getId());
    }

    @Override
    @Transactional
    public RouteResponse updateRoute(Long id, RouteRequest req) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
        Station start = stationRepository.findById(req.getStartStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station", "id", req.getStartStationId()));
        Station end = stationRepository.findById(req.getEndStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station", "id", req.getEndStationId()));

        route.setName(req.getName());
        route.setStartStation(start);
        route.setEndStation(end);
        route.setDistanceKm(req.getDistanceKm());
        route.setEstimatedDurationMinutes(req.getEstimatedDurationMinutes());
        if (req.getStatus() != null) route.setStatus(req.getStatus());
        route.setDescription(req.getDescription());

        if (req.getStops() != null) {
            routeStopRepository.deleteByRouteId(id);
            final Route savedRoute = routeRepository.save(route);
            List<RouteStop> stops = new ArrayList<>();
            for (RouteRequest.RouteStopRequest sr : req.getStops()) {
                Station stopStation = stationRepository.findById(sr.getStationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Station", "id", sr.getStationId()));
                RouteStop stop = RouteStop.builder()
                        .route(savedRoute)
                        .station(stopStation)
                        .stopOrder(sr.getStopOrder())
                        .arrivalOffsetMinutes(sr.getArrivalOffsetMinutes() != null ? sr.getArrivalOffsetMinutes() : 0)
                        .departureOffsetMinutes(sr.getDepartureOffsetMinutes() != null ? sr.getDepartureOffsetMinutes() : 0)
                        .distanceFromStartKm(sr.getDistanceFromStartKm())
                        .build();
                stops.add(stop);
            }
            routeStopRepository.saveAll(stops);
        } else {
            routeRepository.save(route);
        }
        return getRoute(id);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
        return mapToResponse(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAllWithStations().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> getActiveRoutes() {
        return routeRepository.findByStatusWithStations(RouteStatus.ACTIVE).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", id));
        route.setStatus(RouteStatus.INACTIVE);
        routeRepository.save(route);
    }

    public RouteResponse mapToResponse(Route r) {
        RouteResponse res = new RouteResponse();
        res.setId(r.getId());
        res.setName(r.getName());
        res.setRouteCode(r.getRouteCode());
        res.setDistanceKm(r.getDistanceKm());
        res.setEstimatedDurationMinutes(r.getEstimatedDurationMinutes());
        res.setStatus(r.getStatus());
        res.setDescription(r.getDescription());
        res.setCreatedAt(r.getCreatedAt());

        if (r.getStartStation() != null) {
            res.setStartStation(toStationSummary(r.getStartStation()));
        }
        if (r.getEndStation() != null) {
            res.setEndStation(toStationSummary(r.getEndStation()));
        }
        if (r.getStops() != null) {
            res.setStops(r.getStops().stream().map(stop -> {
                RouteResponse.RouteStopResponse sr = new RouteResponse.RouteStopResponse();
                sr.setId(stop.getId());
                sr.setStopOrder(stop.getStopOrder());
                sr.setArrivalOffsetMinutes(stop.getArrivalOffsetMinutes());
                sr.setDepartureOffsetMinutes(stop.getDepartureOffsetMinutes());
                sr.setDistanceFromStartKm(stop.getDistanceFromStartKm());
                if (stop.getStation() != null) {
                    sr.setStation(toStationSummary(stop.getStation()));
                }
                return sr;
            }).collect(Collectors.toList()));
        }
        return res;
    }

    private static RouteResponse.StationSummary toStationSummary(Station s) {
        RouteResponse.StationSummary ss = new RouteResponse.StationSummary();
        ss.setId(s.getId());
        ss.setName(s.getName());
        ss.setCode(s.getCode());
        ss.setCity(s.getCity());
        return ss;
    }
}
