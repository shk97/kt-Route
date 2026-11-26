package com.ktroute.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RouteOperatingCostRequest {
    @NotNull
    private Long routeId;

    @NotNull @Min(1) @Max(12)
    private Integer month;

    @NotNull @Min(2000)
    private Integer year;

    @DecimalMin("0.0") private BigDecimal fuelCost;
    @DecimalMin("0.0") private BigDecimal driverCost;
    @DecimalMin("0.0") private BigDecimal maintenanceCost;
    @DecimalMin("0.0") private BigDecimal tollCost;
    @DecimalMin("0.0") private BigDecimal otherCosts;
    private String notes;
}
