package com.ktroute.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class ProfitabilityResponse {
    private Long routeId;
    private String routeName;
    private String routeCode;
    private BigDecimal totalRevenue;
    private BigDecimal totalOperatingCost;
    private BigDecimal operatingProfit;
    private Double profitMarginPercent;
    private Long passengerCount;
    private Boolean costDataAvailable;
}
