package com.ktroute.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class DashboardStats {
    private long totalPassengers;
    private long totalActiveRoutes;
    private long totalBuses;
    private long totalBookings;
    private long totalPassengersTransported;
    private BigDecimal totalRevenue;
    private double averageOccupancy;
    private long overCapacityRoutes;
    private long underutilizedRoutes;
    private List<StationActivity> busiestStations;
    private List<MonthlyDemand> monthlyDemand;

    @Data @Builder
    public static class StationActivity {
        private Long stationId;
        private String stationName;
        private Long boardingCount;
        private Long alightingCount;
    }

    @Data @Builder
    public static class MonthlyDemand {
        private int month;
        private int year;
        private String monthName;
        private Long bookingCount;
        private Long passengerCount;
        private BigDecimal revenue;
    }
}
