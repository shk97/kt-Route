package com.ktroute.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class RouteDemandResponse {
    private Long routeId;
    private String routeName;
    private String routeCode;
    private String startStation;
    private String endStation;
    private Long passengerCount;
    private Long tripCount;
    private BigDecimal revenue;
    private Double averageOccupancy;
    private Integer totalCapacity;
    private String utilizationCategory; // OVER_CAPACITY, NORMAL, UNDERUTILIZED
}
