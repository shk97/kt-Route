package com.ktroute.app.service;

import com.ktroute.app.dto.request.TripRequest;
import com.ktroute.app.dto.response.TripResponse;
import com.ktroute.app.entity.*;
import com.ktroute.app.enums.RouteStatus;
import com.ktroute.app.enums.TripStatus;
import com.ktroute.app.exception.AppException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.*;
import com.ktroute.app.service.impl.TripServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock private TripRepository                 tripRepository;
    @Mock private RouteRepository                routeRepository;
    @Mock private BusRepository                  busRepository;
    @Mock private DriverRepository               driverRepository;
    @Mock private UserRepository                 userRepository;
    @Mock private BookingRepository              bookingRepository;
    @Mock private ScheduleChangeHistoryRepository changeHistoryRepository;

    @InjectMocks
    private TripServiceImpl tripService;

    private Route  route;
    private Bus    bus;
    private Driver driver;
    private Trip   trip;
    private TripRequest request;

    @BeforeEach
    void setUp() {
        Station stationA = Station.builder().id(1L).name("A").code("STA").city("City A").build();
        Station stationB = Station.builder().id(2L).name("B").code("STB").city("City B").build();

        route = Route.builder()
                .id(1L).name("A-B Express").routeCode("AB")
                .startStation(stationA).endStation(stationB)
                .status(RouteStatus.ACTIVE).build();

        bus = Bus.builder().id(1L).busNumber("BUS-001").totalSeats(40).build();
        driver = Driver.builder().id(1L).fullName("Driver One").licenseNo("L001").build();

        trip = Trip.builder()
                .id(1L).route(route).bus(bus).driver(driver)
                .departureTime(LocalDateTime.now().plusDays(1))
                .arrivalTime(LocalDateTime.now().plusDays(1).plusHours(3))
                .basePrice(new BigDecimal("200.00"))
                .status(TripStatus.SCHEDULED).build();

        request = new TripRequest();
        request.setRouteId(1L);
        request.setBusId(1L);
        request.setDriverId(1L);
        request.setDepartureTime(LocalDateTime.now().plusDays(1));
        request.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(3));
        request.setBasePrice(new BigDecimal("200.00"));
    }

    @Test
    void createTrip_success() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(tripRepository.findConflictingBusTrips(eq(1L), any(), any())).thenReturn(List.of());
        when(tripRepository.findConflictingDriverTrips(eq(1L), any(), any())).thenReturn(List.of());
        when(tripRepository.save(any(Trip.class))).thenReturn(trip);
        when(bookingRepository.countConfirmedPassengersForTrip(1L)).thenReturn(0);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        TripResponse response = tripService.createTrip(request, "admin@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TripStatus.SCHEDULED);
    }

    @Test
    void createTrip_pastDeparture_throwsException() {
        request.setDepartureTime(LocalDateTime.now().minusHours(1));

        assertThatThrownBy(() -> tripService.createTrip(request, "admin@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("future");
    }

    @Test
    void createTrip_arrivalBeforeDeparture_throwsException() {
        request.setArrivalTime(request.getDepartureTime().minusHours(1));

        assertThatThrownBy(() -> tripService.createTrip(request, "admin@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Arrival time");
    }

    @Test
    void createTrip_busConflict_throwsException() {
        when(tripRepository.findConflictingBusTrips(eq(1L), any(), any())).thenReturn(List.of(trip));

        assertThatThrownBy(() -> tripService.createTrip(request, "admin@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Bus already scheduled");
    }

    @Test
    void createTrip_driverConflict_throwsException() {
        when(tripRepository.findConflictingBusTrips(eq(1L), any(), any())).thenReturn(List.of());
        when(tripRepository.findConflictingDriverTrips(eq(1L), any(), any())).thenReturn(List.of(trip));

        assertThatThrownBy(() -> tripService.createTrip(request, "admin@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Driver already scheduled");
    }

    @Test
    void getTrip_success() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.countConfirmedPassengersForTrip(1L)).thenReturn(5);

        TripResponse response = tripService.getTrip(1L);

        assertThat(response).isNotNull();
        assertThat(response.getConfirmedPassengers()).isEqualTo(5);
    }

    @Test
    void getTrip_notFound_throwsException() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTrip(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelTrip_success() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenReturn(trip);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        tripService.cancelTrip(1L, "Maintenance", "admin@test.com");

        assertThat(trip.getStatus()).isEqualTo(TripStatus.CANCELLED);
    }

    @Test
    void cancelTrip_completedTrip_throwsException() {
        trip.setStatus(TripStatus.COMPLETED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.cancelTrip(1L, "reason", "admin@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot cancel a completed trip");
    }

    @Test
    void searchTrips_returnsList() {
        when(tripRepository.searchTrips(eq(1L), eq(2L), any(), any())).thenReturn(List.of(trip));
        when(bookingRepository.countConfirmedPassengersForTrip(1L)).thenReturn(0);

        List<TripResponse> result = tripService.searchTrips(1L, 2L, LocalDate.now().plusDays(1));

        assertThat(result).hasSize(1);
    }

    @Test
    void getTripsByRoute_returnsList() {
        when(tripRepository.findByRouteIdOrderByDepartureTime(1L)).thenReturn(List.of(trip));
        when(bookingRepository.countConfirmedPassengersForTrip(1L)).thenReturn(0);

        List<TripResponse> result = tripService.getTripsByRoute(1L);

        assertThat(result).hasSize(1);
    }
}
