package com.ktroute.app.dto.response;

import com.ktroute.app.enums.PaymentMethod;
import com.ktroute.app.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private String paymentRef;
    private Long bookingId;
    private String bookingRef;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMethod method;
    private String gatewayTransactionId;
    private String failureReason;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    // Razorpay fields — populated on initiate, used by frontend checkout
    private String razorpayOrderId;
    private String razorpayKeyId;
    private String currency;
}
