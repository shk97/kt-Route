package com.ktroute.app.service;

import com.ktroute.app.dto.request.BookingRequest;
import com.ktroute.app.dto.response.BookingResponse;
import com.ktroute.app.entity.*;
import com.ktroute.app.enums.BookingStatus;
import com.ktroute.app.enums.RoleName;
import com.ktroute.app.enums.TripStatus;
import com.ktroute.app.exception.BookingException;
import com.ktroute.app.repository.*;
import com.ktroute.app.service.impl.BookingServiceImpl;
import com.ktroute.app.service.impl.TripServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository  bookingRepository;
    @Mock private TripRepository     tripRepository;
    @Mock private UserRepository     userRepository;
    @Mock private StationRepository  stationRepository;
    @Mock private SeatRepository     seatRepository;
    @Mock private TripServiceImpl    tripService;
    @Mock private EmailService       emailService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User    passenger;
    private Trip    trip;
    private Bus     bus;
    private Route   route;
    private Station stationA, stationB;

    @BeforeEach
    void setUp() {
        Role passengerRole = new Role();
        passengerRole.setId(2L);
        passengerRole.setName(RoleName.ROLE_PASSENGER);

        passenger = new User();
        passenger.setId(1L);
        passenger.setEmail("passenger@test.com");
        passenger.setFullName("Test Passenger");
        passenger.setRoles(Set.of(passengerRole));

        stationA = new Station();
        stationA.setId(1L);
        stationA.setName("Station A");
        stationA.setCode("STA");
        stationA.setCity("City A");

        stationB = new Station();
        stationB.setId(2L);
        stationB.setName("Station B");
        stationB.setCode("STB");
        stationB.setCity("City B");

        route = new Route();
        route.setId(1L);
        route.setName("A-B Express");
        route.setStartStation(stationA);
        route.setEndStation(stationB);

        bus = new Bus();
        bus.setId(1L);
        bus.setBusNumber("BUS-001");
        bus.setTotalSeats(40);

        trip = new Trip();
        trip.setId(1L);
        trip.setRoute(route);
        trip.setBus(bus);
        trip.setStatus(TripStatus.SCHEDULED);
        trip.setDepartureTime(LocalDateTime.now().plusDays(1));
        trip.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(5));
        trip.setBasePrice(new BigDecimal("250.00"));
    }

    @Test
    void createBooking_success() {
        BookingRequest req = buildRequest(1L, 2L, List.of(passengerDetail("Alice", 25, "A1")));

        when(userRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdWithLock(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.countConfirmedPassengersForTrip(1L)).thenReturn(0);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(stationA));
        when(stationRepository.findById(2L)).thenReturn(Optional.of(stationB));
        when(bookingRepository.findActiveBookingsForTrip(1L)).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(100L);
            b.setBookingRef("BK001");
            if (b.getPassengers() == null) b.setPassengers(List.of());
            return b;
        });
        when(tripService.mapToResponse(any())).thenReturn(null);

        BookingResponse response = bookingService.createBooking(req, "passenger@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingRepository, times(2)).save(any(Booking.class));
    }

    @Test
    void createBooking_seatAlreadyBooked_throwsException() {
        BookingRequest req = buildRequest(1L, 2L, List.of(passengerDetail("Alice", 25, "A1")));

        BookingPassenger existingPassenger = new BookingPassenger();
        existingPassenger.setSeatNumber("A1");

        Booking existingBooking = new Booking();
        existingBooking.setStatus(BookingStatus.CONFIRMED);
        existingBooking.setPassengers(List.of(existingPassenger));

        when(userRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdWithLock(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.countConfirmedPassengersForTrip(1L)).thenReturn(0);
        when(bookingRepository.findActiveBookingsForTrip(1L)).thenReturn(List.of(existingBooking));

        assertThatThrownBy(() -> bookingService.createBooking(req, "passenger@test.com"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("A1");
    }

    @Test
    void createBooking_tripNotScheduled_throwsException() {
        trip.setStatus(TripStatus.CANCELLED);
        BookingRequest req = buildRequest(1L, 2L, List.of(passengerDetail("Alice", 25, "A1")));

        when(userRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdWithLock(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> bookingService.createBooking(req, "passenger@test.com"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void createBooking_tripAlreadyDeparted_throwsException() {
        trip.setDepartureTime(LocalDateTime.now().minusHours(1));
        BookingRequest req = buildRequest(1L, 2L, List.of(passengerDetail("Alice", 25, "A1")));

        when(userRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdWithLock(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> bookingService.createBooking(req, "passenger@test.com"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("departed");
    }

    @Test
    void createBooking_notEnoughSeats_throwsException() {
        trip.getBus().setTotalSeats(1);
        BookingRequest req = buildRequest(1L, 2L,
                List.of(passengerDetail("Alice", 25, "A1"), passengerDetail("Bob", 30, "A2")));

        when(userRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdWithLock(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.countConfirmedPassengersForTrip(1L)).thenReturn(0);

        assertThatThrownBy(() -> bookingService.createBooking(req, "passenger@test.com"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("seats");
    }

    @Test
    void cancelBooking_success() {
        Booking booking = buildConfirmedBooking();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(passenger));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.cancelBooking(100L, "Changed plans", "passenger@test.com");

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(emailService).sendCancellationEmail(any(Booking.class));
    }

    @Test
    void cancelBooking_withinTwoHours_throwsException() {
        Booking booking = buildConfirmedBooking();
        booking.getTrip().setDepartureTime(LocalDateTime.now().plusMinutes(90));

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(passenger));

        assertThatThrownBy(() -> bookingService.cancelBooking(100L, "Late cancel", "passenger@test.com"))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("2 hours");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BookingRequest buildRequest(Long boardingId, Long alightingId,
                                        List<BookingRequest.PassengerDetail> passengers) {
        BookingRequest req = new BookingRequest();
        req.setTripId(1L);
        req.setBoardingStationId(boardingId);
        req.setAlightingStationId(alightingId);
        req.setPassengers(passengers);
        return req;
    }

    private BookingRequest.PassengerDetail passengerDetail(String name, int age, String seat) {
        BookingRequest.PassengerDetail pd = new BookingRequest.PassengerDetail();
        pd.setPassengerName(name);
        pd.setAge(age);
        pd.setSeatNumber(seat);
        return pd;
    }

    private Booking buildConfirmedBooking() {
        Booking b = new Booking();
        b.setId(100L);
        b.setBookingRef("BK001");
        b.setUser(passenger);
        b.setTrip(trip);
        b.setStatus(BookingStatus.CONFIRMED);
        b.setPassengerCount(1);
        b.setTotalAmount(new BigDecimal("250.00"));
        b.setPassengers(List.of());
        return b;
    }
}
