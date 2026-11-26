package com.ktroute.app.dto.response;

import com.ktroute.app.enums.RouteStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteResponse {
    private Long id;
    private String name;
    private String routeCode;
    private StationSummary startStation;
    private StationSummary endStation;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private RouteStatus status;
    private String description;
    private List<RouteStopResponse> stops;
    private LocalDateTime createdAt;

    @Data
    public static class RouteStopResponse {
        private Long id;
        private StationSummary station;
        private Integer stopOrder;
        private Integer arrivalOffsetMinutes;
        private Integer departureOffsetMinutes;
        private Double distanceFromStartKm;
    }

    @Data
    public static class StationSummary {
        private Long id;
        private String name;
        private String code;
        private String city;
    }
}
