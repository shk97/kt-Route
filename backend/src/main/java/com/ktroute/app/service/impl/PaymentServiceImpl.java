package com.ktroute.app.service.impl;

import com.ktroute.app.dto.request.PaymentConfirmRequest;
import com.ktroute.app.dto.request.PaymentInitiateRequest;
import com.ktroute.app.dto.response.PaymentResponse;
import com.ktroute.app.entity.Booking;
import com.ktroute.app.entity.Payment;
import com.ktroute.app.enums.BookingStatus;
import com.ktroute.app.enums.PaymentStatus;
import com.ktroute.app.enums.RoleName;
import com.ktroute.app.exception.AppException;
import com.ktroute.app.repository.BookingRepository;
import com.ktroute.app.repository.PaymentRepository;
import com.ktroute.app.repository.UserRepository;
import com.ktroute.app.service.EmailService;
import com.ktroute.app.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository  paymentRepository;
    private final BookingRepository  bookingRepository;
    private final UserRepository     userRepository;
    private final EmailService       emailService;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${app.razorpay.enabled:true}")
    private boolean razorpayEnabled;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BookingRepository bookingRepository,
                              UserRepository userRepository,
                              EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository    = userRepository;
        this.emailService      = emailService;
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentInitiateRequest request, String userEmail) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException("Booking not found", HttpStatus.NOT_FOUND));

        validateOwnership(booking, userEmail);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(
                    "Payment can only be initiated for PENDING bookings. Current status: " + booking.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        if (paymentRepository.existsByBookingIdAndStatus(booking.getId(), PaymentStatus.COMPLETED)) {
            throw new AppException("This booking has already been paid", HttpStatus.CONFLICT);
        }

        // Reuse an existing PROCESSING payment if present
        return paymentRepository.findByBookingId(booking.getId())
                .filter(p -> p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.PROCESSING)
                .map(existing -> {
                    // If it already has a Razorpay order, return it directly
                    if (existing.getRazorpayOrderId() != null) {
                        return toResponse(existing);
                    }
                    // Otherwise create a new Razorpay order and attach it
                    String rzpOrderId = createRazorpayOrder(booking);
                    existing.setRazorpayOrderId(rzpOrderId);
                    existing.setMethod(request.getMethod());
                    return toResponse(paymentRepository.save(existing));
                })
                .orElseGet(() -> {
                    String rzpOrderId = createRazorpayOrder(booking);
                    Payment payment = Payment.builder()
                            .booking(booking)
                            .amount(booking.getTotalAmount())
                            .method(request.getMethod())
                            .status(PaymentStatus.PROCESSING)
                            .razorpayOrderId(rzpOrderId)
                            .build();
                    return toResponse(paymentRepository.save(payment));
                });
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(String paymentRef, PaymentConfirmRequest request, String userEmail) {
        Payment payment = findPayment(paymentRef);
        validateOwnership(payment.getBooking(), userEmail);

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new AppException("Payment already completed", HttpStatus.CONFLICT);
        }
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new AppException("Cannot confirm a " + payment.getStatus() + " payment", HttpStatus.BAD_REQUEST);
        }

        // Verify Razorpay signature: HMAC-SHA256(keySecret, orderId + "|" + paymentId)
        verifyRazorpaySignature(request.getRazorpayOrderId(),
                                request.getRazorpayPaymentId(),
                                request.getRazorpaySignature());

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setGatewayTransactionId(request.getRazorpayPaymentId());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        emailService.sendBookingConfirmationWithTicket(booking);

        log.info("Razorpay payment {} confirmed for booking {}", request.getRazorpayPaymentId(), booking.getBookingRef());
        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse failPayment(String paymentRef, String reason, String userEmail) {
        Payment payment = findPayment(paymentRef);

        boolean isAdmin = userRepository.findByEmail(userEmail)
                .map(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN))
                .orElse(false);
        if (!isAdmin) validateOwnership(payment.getBooking(), userEmail);

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new AppException("Cannot fail an already completed payment", HttpStatus.BAD_REQUEST);
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        log.info("Payment {} marked as FAILED for booking {}", paymentRef, payment.getBooking().getBookingRef());
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentRef, String userEmail) {
        Payment payment = findPayment(paymentRef);
        boolean isAdmin = userRepository.findByEmail(userEmail)
                .map(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN))
                .orElse(false);
        if (!isAdmin) validateOwnership(payment.getBooking(), userEmail);
        return toResponse(payment);
    }

    // ── Razorpay helpers ─────────────────────────────────────────────────────

    private String createRazorpayOrder(Booking booking) {
        if (!razorpayEnabled) {
            // Sandbox bypass: return a fake order ID so local dev works without real keys
            return "order_test_" + booking.getId() + "_" + System.currentTimeMillis();
        }
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderReq = new JSONObject();
            // Razorpay expects amount in paise (1 INR = 100 paise)
            orderReq.put("amount", booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue());
            orderReq.put("currency", "INR");
            orderReq.put("receipt", booking.getBookingRef());
            orderReq.put("payment_capture", 1);
            Order order = client.orders.create(orderReq);
            log.info("Razorpay order {} created for booking {}", order.get("id"), booking.getBookingRef());
            return order.get("id");
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new AppException("Payment gateway error: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    private void verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        if (!razorpayEnabled) {
            // Skip verification in test-bypass mode
            return;
        }
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256"));
            String computed = HexFormat.of().formatHex(mac.doFinal(payload.getBytes()));
            if (!computed.equals(signature)) {
                throw new AppException("Payment verification failed: invalid signature", HttpStatus.BAD_REQUEST);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException("Signature verification error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ── Generic helpers ──────────────────────────────────────────────────────

    private Payment findPayment(String paymentRef) {
        return paymentRepository.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new AppException("Payment not found: " + paymentRef, HttpStatus.NOT_FOUND));
    }

    private void validateOwnership(Booking booking, String email) {
        if (!booking.getUser().getEmail().equals(email)) {
            boolean isAdmin = userRepository.findByEmail(email)
                    .map(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN))
                    .orElse(false);
            if (!isAdmin) {
                throw new AppException("Access denied to this payment", HttpStatus.FORBIDDEN);
            }
        }
    }

    public PaymentResponse toResponse(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setPaymentRef(p.getPaymentRef());
        r.setBookingId(p.getBooking().getId());
        r.setBookingRef(p.getBooking().getBookingRef());
        r.setAmount(p.getAmount());
        r.setStatus(p.getStatus());
        r.setMethod(p.getMethod());
        r.setGatewayTransactionId(p.getGatewayTransactionId());
        r.setFailureReason(p.getFailureReason());
        r.setPaidAt(p.getPaidAt());
        r.setCreatedAt(p.getCreatedAt());
        r.setRazorpayOrderId(p.getRazorpayOrderId());
        r.setRazorpayKeyId(razorpayKeyId);
        r.setCurrency("INR");
        return r;
    }
}
