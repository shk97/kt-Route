package com.ktroute.app.service;

import com.ktroute.app.dto.request.RouteRequest;
import com.ktroute.app.dto.response.RouteResponse;
import com.ktroute.app.entity.Route;
import com.ktroute.app.entity.Station;
import com.ktroute.app.enums.RouteStatus;
import com.ktroute.app.exception.DuplicateResourceException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.RouteRepository;
import com.ktroute.app.repository.RouteStopRepository;
import com.ktroute.app.repository.StationRepository;
import com.ktroute.app.service.impl.RouteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock private RouteRepository routeRepository;
    @Mock private StationRepository stationRepository;
    @Mock private RouteStopRepository routeStopRepository;

    @InjectMocks
    private RouteServiceImpl routeService;

    private Station startStation, endStation;
    private Route route;
    private RouteRequest request;

    @BeforeEach
    void setUp() {
        startStation = Station.builder().id(1L).name("Station A").code("STA").city("City A").isActive(true).build();
        endStation   = Station.builder().id(2L).name("Station B").code("STB").city("City B").isActive(true).build();

        route = Route.builder()
                .id(1L)
                .name("A-B Express")
                .routeCode("AB-EX")
                .startStation(startStation)
                .endStation(endStation)
                .distanceKm(120.0)
                .estimatedDurationMinutes(180)
                .status(RouteStatus.ACTIVE)
                .build();

        request = new RouteRequest();
        request.setName("A-B Express");
        request.setRouteCode("ab-ex");
        request.setStartStationId(1L);
        request.setEndStationId(2L);
        request.setDistanceKm(120.0);
        request.setEstimatedDurationMinutes(180);
    }

    @Test
    void createRoute_success() {
        when(routeRepository.existsByRouteCode("AB-EX")).thenReturn(false);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(startStation));
        when(stationRepository.findById(2L)).thenReturn(Optional.of(endStation));
        when(routeRepository.save(any(Route.class))).thenReturn(route);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        RouteResponse response = routeService.createRoute(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("A-B Express");
        assertThat(response.getRouteCode()).isEqualTo("AB-EX");
        assertThat(response.getStatus()).isEqualTo(RouteStatus.ACTIVE);
    }

    @Test
    void createRoute_duplicateCode_throwsException() {
        when(routeRepository.existsByRouteCode("AB-EX")).thenReturn(true);

        assertThatThrownBy(() -> routeService.createRoute(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ab-ex");
    }

    @Test
    void createRoute_startStationNotFound_throwsException() {
        when(routeRepository.existsByRouteCode("AB-EX")).thenReturn(false);
        when(stationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.createRoute(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRoute_success() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        RouteResponse response = routeService.getRoute(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRouteCode()).isEqualTo("AB-EX");
    }

    @Test
    void getRoute_notFound_throwsException() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.getRoute(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllRoutes_returnsList() {
        when(routeRepository.findAllWithStations()).thenReturn(List.of(route));

        List<RouteResponse> result = routeService.getAllRoutes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("A-B Express");
    }

    @Test
    void getActiveRoutes_returnsActiveOnly() {
        when(routeRepository.findByStatusWithStations(RouteStatus.ACTIVE)).thenReturn(List.of(route));

        List<RouteResponse> result = routeService.getActiveRoutes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(RouteStatus.ACTIVE);
    }

    @Test
    void deactivateRoute_success() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

        routeService.deactivateRoute(1L);

        assertThat(route.getStatus()).isEqualTo(RouteStatus.INACTIVE);
        verify(routeRepository).save(route);
    }

    @Test
    void deactivateRoute_notFound_throwsException() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.deactivateRoute(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
