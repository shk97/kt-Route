package com.ktroute.app.dto.response;

import com.ktroute.app.enums.TripStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TripResponse {
    private Long id;
    private RouteResponse route;
    private RouteResponse.StationSummary startStation;
    private RouteResponse.StationSummary endStation;
    private String routeName;
    private Long routeId;
    private BusResponse bus;
    private DriverResponse driver;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal basePrice;
    private TripStatus status;
    private String notes;
    private Integer availableSeats;
    private Integer totalSeats;
    private Integer confirmedPassengers;
    private LocalDateTime createdAt;
}
