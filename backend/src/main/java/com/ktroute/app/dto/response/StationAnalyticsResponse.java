package com.ktroute.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

public class StationAnalyticsResponse {

    @Data @Builder
    public static class Overview {
        private long totalActiveStations;
        private long totalBoardingPassengers;
        private String busiestStation;
        private long busiestStationPassengers;
        private String leastBusyStation;
        private long leastBusyStationPassengers;
        private long totalRoutesAnalysed;
        private double avgBoardingPerStation;
    }

    @Data @Builder
    public static class StationBoarding {
        private Long stationId;
        private String stationName;
        private String city;
        private String state;
        private Double latitude;
        private Double longitude;
        private long totalBoarding;
        private long routeCount;
        private long tripCount;
        private int rank;
        private String demandLevel; // HIGH / MEDIUM / LOW
    }

    @Data @Builder
    public static class RouteBreakdown {
        private Long routeId;
        private String routeName;
        private String origin;
        private String destination;
        private long passengers;
        private long tripCount;
        private double avgPerTrip;
        private double pctOfStation; // % of station's total boarding
    }

    @Data @Builder
    public static class TripBoarding {
        private Long tripId;
        private String busNumber;
        private String departureTime;
        private long boardingPassengers;
    }

    @Data @Builder
    public static class PeakHour {
        private int hour;
        private String label; // "06:00–07:00"
        private long passengers;
    }

    @Data @Builder
    public static class DailyTrend {
        private String date;
        private long passengers;
    }

    @Data @Builder
    public static class Recommendation {
        private String stationName;
        private String routeName;       // nullable
        private long passengers;
        private String pattern;
        private String suggestedAction;
        private String reason;
        private String severity;        // HIGH / MEDIUM / LOW
    }

    @Data @Builder
    public static class MapStation {
        private Long stationId;
        private String stationName;
        private String city;
        private String state;
        private Double latitude;
        private Double longitude;
        private long boardingPassengers;
        private long routeCount;
        private String demandLevel;
    }
}
