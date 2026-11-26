package com.ktroute.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TripRequest {
    @NotNull
    private Long routeId;

    @NotNull
    private Long busId;

    private Long driverId;

    @NotNull
    private LocalDateTime departureTime;

    @NotNull
    private LocalDateTime arrivalTime;

    @NotNull @DecimalMin("0.0")
    private BigDecimal basePrice;

    @Size(max=500)
    private String notes;

    @Size(max=500)
    private String changeReason;
}
