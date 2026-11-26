package com.ktroute.app.dto.request;

import com.ktroute.app.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentInitiateRequest {

    @NotNull
    private Long bookingId;

    @NotNull
    private PaymentMethod method;
}
