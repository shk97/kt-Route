package com.ktroute.app.service;

import com.ktroute.app.dto.request.PaymentConfirmRequest;
import com.ktroute.app.dto.request.PaymentInitiateRequest;
import com.ktroute.app.dto.response.PaymentResponse;
import com.ktroute.app.entity.*;
import com.ktroute.app.enums.*;
import com.ktroute.app.exception.AppException;
import com.ktroute.app.repository.BookingRepository;
import com.ktroute.app.repository.PaymentRepository;
import com.ktroute.app.repository.UserRepository;
import com.ktroute.app.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository  paymentRepository;
    @Mock private BookingRepository  bookingRepository;
    @Mock private UserRepository     userRepository;
    @Mock private EmailService       emailService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User    passenger;
    private Booking booking;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setName(RoleName.ROLE_PASSENGER);

        passenger = new User();
        passenger.setId(1L);
        passenger.setEmail("passenger@test.com");
        passenger.setFullName("Test Passenger");
        passenger.setRoles(Set.of(role));

        Trip trip = new Trip();
        trip.setId(1L);
        Route route = new Route();
        route.setName("Test Route");
        trip.setRoute(route);
        trip.setDepartureTime(LocalDateTime.now().plusDays(1));

        booking = new Booking();
        booking.setId(10L);
        booking.setBookingRef("BK001");
        booking.setUser(passenger);
        booking.setTrip(trip);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("500.00"));
    }

    @Test
    void initiatePayment_success() {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setBookingId(10L);
        req.setMethod(PaymentMethod.UPI);

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingIdAndStatus(10L, PaymentStatus.COMPLETED)).thenReturn(false);
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaymentResponse response = paymentService.initiatePayment(req, "passenger@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void initiatePayment_bookingNotPending_throwsException() {
        booking.setStatus(BookingStatus.CONFIRMED);
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setBookingId(10L);
        req.setMethod(PaymentMethod.CREDIT_CARD);

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.initiatePayment(req, "passenger@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void initiatePayment_alreadyPaid_throwsException() {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setBookingId(10L);
        req.setMethod(PaymentMethod.DEBIT_CARD);

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingIdAndStatus(10L, PaymentStatus.COMPLETED)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.initiatePayment(req, "passenger@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("already been paid");
    }

    @Test
    void confirmPayment_success() {
        Payment payment = Payment.builder()
                .id(1L)
                .paymentRef("PAY001")
                .booking(booking)
                .amount(new BigDecimal("500.00"))
                .method(PaymentMethod.UPI)
                .status(PaymentStatus.PROCESSING)
                .build();

        when(paymentRepository.findByPaymentRef("PAY001")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.confirmPayment("PAY001", new PaymentConfirmRequest(), "passenger@test.com");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(emailService).sendBookingConfirmationWithTicket(booking);
    }

    @Test
    void confirmPayment_alreadyCompleted_throwsException() {
        Payment payment = Payment.builder()
                .id(1L)
                .paymentRef("PAY001")
                .booking(booking)
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByPaymentRef("PAY001")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment("PAY001", new PaymentConfirmRequest(), "passenger@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void failPayment_success() {
        Payment payment = Payment.builder()
                .id(1L)
                .paymentRef("PAY001")
                .booking(booking)
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.PROCESSING)
                .build();

        when(paymentRepository.findByPaymentRef("PAY001")).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(passenger));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.failPayment("PAY001", "Card declined", "passenger@test.com");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getFailureReason()).isEqualTo("Card declined");
    }

    @Test
    void failPayment_completedPayment_throwsException() {
        Payment payment = Payment.builder()
                .id(1L)
                .paymentRef("PAY001")
                .booking(booking)
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.findByPaymentRef("PAY001")).thenReturn(Optional.of(payment));
        when(userRepository.findByEmail("passenger@test.com")).thenReturn(Optional.of(passenger));

        assertThatThrownBy(() -> paymentService.failPayment("PAY001", "reason", "passenger@test.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("completed");
    }
}
