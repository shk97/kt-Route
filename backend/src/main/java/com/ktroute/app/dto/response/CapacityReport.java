package com.ktroute.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class CapacityReport {
    private Long tripId;
    private String routeName;
    private String busNumber;
    private LocalDateTime departureTime;
    private Integer totalSeats;
    private Integer confirmedPassengers;
    private Double occupancyPercentage;
    private String utilizationCategory;
    private String recommendation;
}
