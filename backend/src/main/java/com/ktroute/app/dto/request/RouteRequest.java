package com.ktroute.app.dto.request;

import com.ktroute.app.enums.RouteStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class RouteRequest {
    @NotBlank @Size(min=3, max=150)
    private String name;

    @NotBlank @Size(min=2, max=20)
    private String routeCode;

    @NotNull
    private Long startStationId;

    @NotNull
    private Long endStationId;

    @NotNull @Positive
    private Double distanceKm;

    @NotNull @Positive
    private Integer estimatedDurationMinutes;

    private RouteStatus status;

    @Size(max=500)
    private String description;

    private List<RouteStopRequest> stops;

    @Data
    public static class RouteStopRequest {
        @NotNull
        private Long stationId;
        @NotNull @Min(0)
        private Integer stopOrder;
        @Min(0)
        private Integer arrivalOffsetMinutes;
        @Min(0)
        private Integer departureOffsetMinutes;
        private Double distanceFromStartKm;
    }
}
