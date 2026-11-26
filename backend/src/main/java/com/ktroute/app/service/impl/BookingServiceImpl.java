package com.ktroute.app.service.impl;

import com.ktroute.app.dto.request.BookingRequest;
import com.ktroute.app.dto.response.BookingResponse;
import com.ktroute.app.dto.response.RouteResponse;
import com.ktroute.app.entity.*;
import com.ktroute.app.enums.BookingStatus;
import com.ktroute.app.enums.RoleName;
import com.ktroute.app.exception.AppException;
import com.ktroute.app.exception.BookingException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.*;
import com.ktroute.app.service.BookingService;
import com.ktroute.app.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository    tripRepository;
    private final UserRepository    userRepository;
    private final StationRepository stationRepository;
    private final TripServiceImpl   tripService;
    private final EmailService      emailService;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest req, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        // Pessimistic write lock — prevents two concurrent requests from both passing the seat check
        Trip trip = tripRepository.findByIdWithLock(req.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", req.getTripId()));

        if (trip.getStatus() != com.ktroute.app.enums.TripStatus.SCHEDULED) {
            throw new BookingException("Trip is not available for booking");
        }
        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BookingException("Cannot book a trip that has already departed");
        }

        int busCapacity = trip.getBus().getTotalSeats();
        Integer confirmed = bookingRepository.countConfirmedPassengersForTrip(trip.getId());
        int booked    = confirmed != null ? confirmed : 0;
        int requested = req.getPassengers().size();
        if (booked + requested > busCapacity) {
            throw new BookingException("Not enough seats. Available: " + (busCapacity - booked));
        }

        Set<String> requestedSeats = req.getPassengers().stream()
                .map(BookingRequest.PassengerDetail::getSeatNumber)
                .collect(Collectors.toSet());
        if (requestedSeats.size() != req.getPassengers().size()) {
            throw new BookingException("Duplicate seat numbers in request");
        }

        // Seat conflict check runs inside the pessimistic lock — no race condition
        Set<String> occupiedSeats = bookingRepository.findActiveBookingsForTrip(trip.getId())
                .stream()
                .flatMap(b -> b.getPassengers().stream())
                .map(BookingPassenger::getSeatNumber)
                .collect(Collectors.toSet());
        for (String seat : requestedSeats) {
            if (occupiedSeats.contains(seat)) {
                throw new BookingException("Seat " + seat + " is already booked");
            }
        }

        Station boarding = stationRepository.findById(req.getBoardingStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station", "id", req.getBoardingStationId()));
        Station alighting = stationRepository.findById(req.getAlightingStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station", "id", req.getAlightingStationId()));

        if (boarding.getId().equals(alighting.getId())) {
            throw new BookingException("Boarding and alighting stations must differ");
        }

        BigDecimal totalAmount = trip.getBasePrice().multiply(BigDecimal.valueOf(requested));

        // Booking is PENDING until payment is confirmed by PaymentService
        Booking booking = Booking.builder()
                .user(user)
                .trip(trip)
                .passengerCount(requested)
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .boardingStation(boarding)
                .alightingStation(alighting)
                .build();
        booking = bookingRepository.save(booking);

        final Booking savedBooking = booking;
        List<BookingPassenger> passengers = req.getPassengers().stream().map(pd ->
                BookingPassenger.builder()
                        .booking(savedBooking)
                        .passengerName(pd.getPassengerName().trim())
                        .age(pd.getAge())
                        .seatNumber(pd.getSeatNumber().trim().toUpperCase())
                        .ticketPrice(trip.getBasePrice())
                        .build()
        ).collect(Collectors.toList());
        booking.setPassengers(passengers);
        return mapToResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id, String userEmail) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        validateAccess(booking, userEmail);
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByRef(String ref, String userEmail) {
        Booking booking = bookingRepository.findByBookingRef(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "ref", ref));
        validateAccess(booking, userEmail);
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id, String reason, String userEmail) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        validateAccess(booking, userEmail);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingException("Cannot cancel a completed booking");
        }

        boolean isAdmin = isAdminUser(userEmail);
        if (!isAdmin && booking.getTrip().getDepartureTime().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BookingException("Cannot cancel booking within 2 hours of departure");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        emailService.sendCancellationEmail(saved);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByTrip(Long tripId) {
        return bookingRepository.findByTripId(tripId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private boolean isAdminUser(String email) {
        return userRepository.findByEmail(email)
                .map(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN))
                .orElse(false);
    }

    private void validateAccess(Booking booking, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin && !booking.getUser().getId().equals(user.getId())) {
            throw new AppException("Access denied to this booking", HttpStatus.FORBIDDEN);
        }
    }

    public BookingResponse mapToResponse(Booking b) {
        BookingResponse res = new BookingResponse();
        res.setId(b.getId());
        res.setBookingRef(b.getBookingRef());
        res.setPassengerCount(b.getPassengerCount());
        res.setTotalAmount(b.getTotalAmount());
        res.setStatus(b.getStatus());
        res.setCancellationReason(b.getCancellationReason());
        res.setCancelledAt(b.getCancelledAt());
        res.setCreatedAt(b.getCreatedAt());

        if (b.getBoardingStation() != null)  res.setBoardingStation(toStationSummary(b.getBoardingStation()));
        if (b.getAlightingStation() != null) res.setAlightingStation(toStationSummary(b.getAlightingStation()));
        if (b.getTrip() != null)             res.setTrip(tripService.mapToResponse(b.getTrip()));

        if (b.getPassengers() != null) {
            res.setPassengers(b.getPassengers().stream().map(p -> {
                BookingResponse.PassengerDetail pd = new BookingResponse.PassengerDetail();
                pd.setId(p.getId());
                pd.setPassengerName(p.getPassengerName());
                pd.setAge(p.getAge());
                pd.setSeatNumber(p.getSeatNumber());
                pd.setTicketPrice(p.getTicketPrice());
                return pd;
            }).collect(Collectors.toList()));
        }
        return res;
    }

    private RouteResponse.StationSummary toStationSummary(Station s) {
        RouteResponse.StationSummary ss = new RouteResponse.StationSummary();
        ss.setId(s.getId());
        ss.setName(s.getName());
        ss.setCode(s.getCode());
        ss.setCity(s.getCity());
        return ss;
    }
}
