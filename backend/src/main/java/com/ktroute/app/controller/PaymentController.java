package com.ktroute.app.controller;

import com.ktroute.app.dto.request.PaymentConfirmRequest;
import com.ktroute.app.dto.request.PaymentInitiateRequest;
import com.ktroute.app.dto.response.ApiResponse;
import com.ktroute.app.dto.response.PaymentResponse;
import com.ktroute.app.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Step 1 — Passenger initiates payment.
     * Creates a Razorpay Order and returns the order details for the frontend checkout.
     */
    @PostMapping("/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiate(
            @Valid @RequestBody PaymentInitiateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Razorpay order created — open checkout",
                paymentService.initiatePayment(request, userDetails.getUsername())));
    }

    /**
     * Step 2 — Verify Razorpay signature and confirm payment.
     * On success: booking moves to CONFIRMED and e-ticket is emailed.
     */
    @PostMapping("/{paymentRef}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirm(
            @PathVariable String paymentRef,
            @Valid @RequestBody PaymentConfirmRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment confirmed — booking is now CONFIRMED. Ticket emailed.",
                paymentService.confirmPayment(paymentRef, request, userDetails.getUsername())));
    }

    /**
     * Step 3 (error path) — Mark payment as FAILED.
     */
    @PostMapping("/{paymentRef}/fail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> fail(
            @PathVariable String paymentRef,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment marked as failed",
                paymentService.failPayment(paymentRef, body.get("reason"), userDetails.getUsername())));
    }

    @GetMapping("/{paymentRef}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable String paymentRef,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPayment(paymentRef, userDetails.getUsername())));
    }
}
