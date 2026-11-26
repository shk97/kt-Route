package com.ktroute.app.service;

import com.ktroute.app.dto.request.PaymentConfirmRequest;
import com.ktroute.app.dto.request.PaymentInitiateRequest;
import com.ktroute.app.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse initiatePayment(PaymentInitiateRequest request, String userEmail);
    PaymentResponse confirmPayment(String paymentRef, PaymentConfirmRequest request, String userEmail);
    PaymentResponse failPayment(String paymentRef, String reason, String userEmail);
    PaymentResponse getPayment(String paymentRef, String userEmail);
}
