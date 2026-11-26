package com.ktroute.app.service.impl;

import com.ktroute.app.dto.response.*;
import com.ktroute.app.entity.Trip;
import com.ktroute.app.enums.BookingStatus;
import com.ktroute.app.enums.RouteStatus;
import com.ktroute.app.repository.*;
import com.ktroute.app.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final BookingRepository bookingRepository;
    private final BoardingRecordRepository boardingRecordRepository;
    private final TripRepository tripRepository;
    private final RouteOperatingCostRepository costRepository;
    private final StationRepository stationRepository;

    @Value("${app.occupancy.underutilized-threshold:40}")
    private int underutilizedThreshold;

    @Value("${app.occupancy.overcapacity-threshold:100}")
    private int overcapacityThreshold;

    @Override
    public DashboardStats getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yearStart = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        long totalPassengers = userRepository.countPassengers();
        long totalActiveRoutes = routeRepository.countByStatus(RouteStatus.ACTIVE);
        long totalBuses = busRepository.count();
        long totalBookings = bookingRepository.countByStatusIn(List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));
        BigDecimal totalRevenue = bookingRepository.sumRevenueBetween(yearStart, now);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        List<Object[]> monthlyRaw = bookingRepository.findMonthlyDemand(yearStart, now);
        List<DashboardStats.MonthlyDemand> monthly = monthlyRaw.stream().map(row -> {
            int m = ((Number) row[0]).intValue();
            int y = ((Number) row[1]).intValue();
            return DashboardStats.MonthlyDemand.builder()
                    .month(m).year(y)
                    .monthName(Month.of(m).name())
                    .bookingCount(((Number) row[2]).longValue())
                    .passengerCount(((Number) row[3]).longValue())
                    .revenue(row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());

        long transported = monthly.stream().mapToLong(DashboardStats.MonthlyDemand::getPassengerCount).sum();

        List<Trip> trips = tripRepository.findScheduledTripsInRange(yearStart, now);
        double avgOccupancy = computeAvgOccupancy(trips);

        List<DashboardStats.StationActivity> busiestStations = getStationActivity(yearStart, now)
                .stream().limit(5).collect(Collectors.toList());

        return DashboardStats.builder()
                .totalPassengers(totalPassengers)
                .totalActiveRoutes(totalActiveRoutes)
                .totalBuses(totalBuses)
                .totalBookings(totalBookings)
                .totalPassengersTransported(transported)
                .totalRevenue(totalRevenue)
                .averageOccupancy(Math.round(avgOccupancy * 10.0) / 10.0)
                .busiestStations(busiestStations)
                .monthlyDemand(monthly)
                .build();
    }

    @Override
    public List<RouteDemandResponse> getRouteDemand(LocalDateTime from, LocalDateTime to, String sortBy) {
        List<Object[]> raw = bookingRepository.findRouteWiseDemand(from, to);
        if (raw.isEmpty()) return List.of();

        List<Trip> allTrips = tripRepository.findScheduledTripsInRange(from, to);
        Map<Long, List<Trip>> tripsByRoute = allTrips.stream()
                .collect(Collectors.groupingBy(t -> t.getRoute().getId()));

        Map<Long, Integer> countMap = batchCountMap(allTrips);

        List<RouteDemandResponse> result = raw.stream().map(row -> {
            Long routeId = ((Number) row[0]).longValue();
            long passengers = ((Number) row[3]).longValue();
            BigDecimal revenue = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;

            List<Trip> routeTrips = tripsByRoute.getOrDefault(routeId, List.of());
            double avgOcc = routeTrips.stream().mapToDouble(t -> {
                int cap = t.getBus().getTotalSeats();
                int conf = countMap.getOrDefault(t.getId(), 0);
                return cap > 0 ? conf * 100.0 / cap : 0;
            }).average().orElse(0.0);

            return RouteDemandResponse.builder()
                    .routeId(routeId)
                    .routeName(row[1] != null ? row[1].toString() : "")
                    .passengerCount(passengers)
                    .tripCount(((Number) row[2]).longValue())
                    .revenue(revenue)
                    .averageOccupancy(Math.round(avgOcc * 10.0) / 10.0)
                    .utilizationCategory(utilizationCategory(avgOcc))
                    .build();
        }).collect(Collectors.toList());

        Comparator<RouteDemandResponse> comparator = switch (sortBy) {
            case "occupancy_asc"  -> Comparator.comparingDouble(RouteDemandResponse::getAverageOccupancy);
            case "occupancy_desc" -> Comparator.comparingDouble(RouteDemandResponse::getAverageOccupancy).reversed();
            case "passengers_asc" -> Comparator.comparingLong(RouteDemandResponse::getPassengerCount);
            default               -> Comparator.comparingLong(RouteDemandResponse::getPassengerCount).reversed(); // passengers_desc
        };
        result.sort(comparator);
        return result;
    }

    @Override
    public List<DashboardStats.StationActivity> getStationActivity(LocalDateTime from, LocalDateTime to) {
        List<Object[]> boardingRaw = boardingRecordRepository.findBoardingStatsByStation(from, to);
        return boardingRaw.stream().map(row -> DashboardStats.StationActivity.builder()
                .stationId(((Number) row[0]).longValue())
                .stationName(row[1].toString())
                .boardingCount(((Number) row[2]).longValue())
                .alightingCount(0L)
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<CapacityReport> getCapacityReport(LocalDateTime from, LocalDateTime to) {
        List<Trip> trips = tripRepository.findScheduledTripsInRange(from, to);
        if (trips.isEmpty()) return List.of();

        Map<Long, Integer> countMap = batchCountMap(trips);

        return trips.stream().map(t -> {
            int cap = t.getBus().getTotalSeats();
            int passengers = countMap.getOrDefault(t.getId(), 0);
            double occ = cap > 0 ? passengers * 100.0 / cap : 0;
            String recommendation = occ > overcapacityThreshold ?
                    "Deploy larger bus or add additional trip" :
                    occ < underutilizedThreshold ?
                    "Consider vehicle reassignment or schedule review" : "Normal operation";

            return CapacityReport.builder()
                    .tripId(t.getId())
                    .routeName(t.getRoute().getName())
                    .busNumber(t.getBus().getBusNumber())
                    .departureTime(t.getDepartureTime())
                    .totalSeats(cap)
                    .confirmedPassengers(passengers)
                    .occupancyPercentage(Math.round(occ * 10.0) / 10.0)
                    .utilizationCategory(utilizationCategory(occ))
                    .recommendation(recommendation)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<DashboardStats.MonthlyDemand> getMonthlyDemand(int fromYear, int toYear) {
        LocalDateTime from = LocalDateTime.of(fromYear, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(toYear, 12, 31, 23, 59, 59);
        List<Object[]> raw = bookingRepository.findMonthlyDemand(from, to);
        return raw.stream().map(row -> {
            int m = ((Number) row[0]).intValue();
            int y = ((Number) row[1]).intValue();
            return DashboardStats.MonthlyDemand.builder()
                    .month(m).year(y)
                    .monthName(Month.of(m).name())
                    .bookingCount(((Number) row[2]).longValue())
                    .passengerCount(((Number) row[3]).longValue())
                    .revenue(row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<DashboardStats.MonthlyDemand> getMonthlyDemandForRoute(Long routeId, int fromYear, int toYear) {
        LocalDateTime from = LocalDateTime.of(fromYear, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(toYear, 12, 31, 23, 59, 59);
        List<Object[]> raw = bookingRepository.findMonthlyDemandByRoute(routeId, from, to);
        return raw.stream().map(row -> {
            int m = ((Number) row[0]).intValue();
            int y = ((Number) row[1]).intValue();
            return DashboardStats.MonthlyDemand.builder()
                    .month(m).year(y)
                    .monthName(Month.of(m).name())
                    .bookingCount(((Number) row[2]).longValue())
                    .passengerCount(((Number) row[3]).longValue())
                    .revenue(row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProfitabilityResponse> getProfitability(LocalDateTime from, LocalDateTime to) {
        List<Object[]> revenueData = bookingRepository.findRouteWiseDemand(from, to);
        if (revenueData.isEmpty()) return List.of();

        Map<Long, BigDecimal> costMap = costRepository.sumCostByRouteInRange(
                        from.getYear(), from.getMonthValue(),
                        to.getYear(), to.getMonthValue())
                .stream().collect(Collectors.toMap(
                        c -> ((Number) c[0]).longValue(),
                        c -> c[1] != null ? new BigDecimal(c[1].toString()) : BigDecimal.ZERO
                ));

        return revenueData.stream().map(row -> {
            Long routeId = ((Number) row[0]).longValue();
            BigDecimal revenue = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;
            BigDecimal totalCost = costMap.get(routeId);
            BigDecimal profit = totalCost != null ? revenue.subtract(totalCost) : null;
            Double margin = (totalCost != null && revenue.compareTo(BigDecimal.ZERO) > 0) ?
                    profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : null;

            return ProfitabilityResponse.builder()
                    .routeId(routeId)
                    .routeName(row[1] != null ? row[1].toString() : "")
                    .totalRevenue(revenue)
                    .totalOperatingCost(totalCost)
                    .operatingProfit(profit)
                    .profitMarginPercent(margin)
                    .passengerCount(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                    .costDataAvailable(totalCost != null)
                    .build();
        }).collect(Collectors.toList());
    }

    private Map<Long, Integer> batchCountMap(List<Trip> trips) {
        if (trips.isEmpty()) return Map.of();
        List<Long> ids = trips.stream().map(Trip::getId).collect(Collectors.toList());
        return bookingRepository.countConfirmedPassengersForTripIds(ids)
                .stream().collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).intValue()
                ));
    }

    private double computeAvgOccupancy(List<Trip> trips) {
        if (trips.isEmpty()) return 0.0;
        Map<Long, Integer> countMap = batchCountMap(trips);
        return trips.stream().mapToDouble(t -> {
            int cap = t.getBus().getTotalSeats();
            int conf = countMap.getOrDefault(t.getId(), 0);
            return cap > 0 ? conf * 100.0 / cap : 0;
        }).average().orElse(0.0);
    }

    private String utilizationCategory(double occ) {
        if (occ > overcapacityThreshold) return "OVER_CAPACITY";
        if (occ < underutilizedThreshold) return "UNDERUTILIZED";
        return "NORMAL";
    }

    // ── Station Boarding Analytics ────────────────────────────────────────────

    @Override
    public StationAnalyticsResponse.Overview getStationOverview(LocalDateTime from, LocalDateTime to) {
        long activeStations = stationRepository.findByIsActiveTrue().size();
        Long total = boardingRecordRepository.sumTotalBoarding(from, to);
        long totalBoarding = total != null ? total : 0L;

        List<StationAnalyticsResponse.StationBoarding> ranked = getStationBoardingRanked(from, to);
        String busiest = ranked.isEmpty() ? "N/A" : ranked.get(0).getStationName();
        long busiestPax = ranked.isEmpty() ? 0L : ranked.get(0).getTotalBoarding();
        String leastBusy = ranked.isEmpty() ? "N/A" : ranked.get(ranked.size() - 1).getStationName();
        long leastPax = ranked.isEmpty() ? 0L : ranked.get(ranked.size() - 1).getTotalBoarding();
        long routeCount = ranked.stream().mapToLong(StationAnalyticsResponse.StationBoarding::getRouteCount).sum();
        double avg = ranked.isEmpty() ? 0.0 : (double) totalBoarding / ranked.size();

        return StationAnalyticsResponse.Overview.builder()
                .totalActiveStations(activeStations)
                .totalBoardingPassengers(totalBoarding)
                .busiestStation(busiest)
                .busiestStationPassengers(busiestPax)
                .leastBusyStation(leastBusy)
                .leastBusyStationPassengers(leastPax)
                .totalRoutesAnalysed(routeCount)
                .avgBoardingPerStation(Math.round(avg * 10.0) / 10.0)
                .build();
    }

    @Override
    public List<StationAnalyticsResponse.StationBoarding> getStationBoardingRanked(LocalDateTime from, LocalDateTime to) {
        List<Object[]> raw = boardingRecordRepository.findStationBoardingRanked(from, to);
        long maxBoarding = raw.isEmpty() ? 1L : ((Number) raw.get(0)[6]).longValue();
        List<StationAnalyticsResponse.StationBoarding> result = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Object[] row = raw.get(i);
            long boarding = ((Number) row[6]).longValue();
            result.add(StationAnalyticsResponse.StationBoarding.builder()
                    .stationId(((Number) row[0]).longValue())
                    .stationName(row[1] != null ? row[1].toString() : "")
                    .city(row[2] != null ? row[2].toString() : "")
                    .state(row[3] != null ? row[3].toString() : "")
                    .latitude(row[4] != null ? ((Number) row[4]).doubleValue() : null)
                    .longitude(row[5] != null ? ((Number) row[5]).doubleValue() : null)
                    .totalBoarding(boarding)
                    .routeCount(((Number) row[7]).longValue())
                    .tripCount(((Number) row[8]).longValue())
                    .rank(i + 1)
                    .demandLevel(boardingDemandLevel(boarding, maxBoarding))
                    .build());
        }
        return result;
    }

    @Override
    public List<StationAnalyticsResponse.RouteBreakdown> getStationRouteBreakdown(Long stationId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> raw = boardingRecordRepository.findRouteBreakdownForStation(stationId, from, to);
        long stationTotal = raw.stream().mapToLong(r -> ((Number) r[4]).longValue()).sum();
        return raw.stream().map(row -> {
            long pax  = ((Number) row[4]).longValue();
            long trips = ((Number) row[5]).longValue();
            double pct = stationTotal > 0 ? (pax * 100.0 / stationTotal) : 0;
            return StationAnalyticsResponse.RouteBreakdown.builder()
                    .routeId(((Number) row[0]).longValue())
                    .routeName(row[1] != null ? row[1].toString() : "")
                    .origin(row[2] != null ? row[2].toString() : "")
                    .destination(row[3] != null ? row[3].toString() : "")
                    .passengers(pax)
                    .tripCount(trips)
                    .avgPerTrip(trips > 0 ? Math.round((double) pax / trips * 10.0) / 10.0 : 0)
                    .pctOfStation(Math.round(pct * 10.0) / 10.0)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<StationAnalyticsResponse.TripBoarding> getStationTripBreakdown(Long stationId, Long routeId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> raw = boardingRecordRepository.findTripBreakdownForStationRoute(stationId, routeId, from, to);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        return raw.stream().map(row -> StationAnalyticsResponse.TripBoarding.builder()
                .tripId(((Number) row[0]).longValue())
                .busNumber(row[1] != null ? row[1].toString() : "")
                .departureTime(row[2] != null ? ((LocalDateTime) row[2]).format(fmt) : "")
                .boardingPassengers(((Number) row[3]).longValue())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<StationAnalyticsResponse.PeakHour> getStationPeakHours(Long stationId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> raw = boardingRecordRepository.findPeakHoursForStation(stationId, from, to);
        return raw.stream().map(row -> {
            int hr = ((Number) row[0]).intValue();
            return StationAnalyticsResponse.PeakHour.builder()
                    .hour(hr)
                    .label(String.format("%02d:00–%02d:00", hr, hr + 1))
                    .passengers(((Number) row[1]).longValue())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<StationAnalyticsResponse.DailyTrend> getDailyTrend(Long stationId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> raw = boardingRecordRepository.findDailyTrend(stationId, from, to);
        return raw.stream().map(row -> StationAnalyticsResponse.DailyTrend.builder()
                .date(row[0] != null ? row[0].toString() : "")
                .passengers(((Number) row[1]).longValue())
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<StationAnalyticsResponse.MapStation> getMapStations(LocalDateTime from, LocalDateTime to) {
        List<StationAnalyticsResponse.StationBoarding> ranked = getStationBoardingRanked(from, to);
        return ranked.stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .map(s -> StationAnalyticsResponse.MapStation.builder()
                        .stationId(s.getStationId())
                        .stationName(s.getStationName())
                        .city(s.getCity())
                        .state(s.getState())
                        .latitude(s.getLatitude())
                        .longitude(s.getLongitude())
                        .boardingPassengers(s.getTotalBoarding())
                        .routeCount(s.getRouteCount())
                        .demandLevel(s.getDemandLevel())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<StationAnalyticsResponse.Recommendation> getRecommendations(LocalDateTime from, LocalDateTime to) {
        List<StationAnalyticsResponse.StationBoarding> ranked = getStationBoardingRanked(from, to);
        if (ranked.isEmpty()) return List.of();

        long totalPax = ranked.stream().mapToLong(StationAnalyticsResponse.StationBoarding::getTotalBoarding).sum();
        long avgPax   = totalPax / ranked.size();
        List<StationAnalyticsResponse.Recommendation> recs = new ArrayList<>();

        for (StationAnalyticsResponse.StationBoarding s : ranked) {
            List<StationAnalyticsResponse.RouteBreakdown> routes = getStationRouteBreakdown(s.getStationId(), from, to);

            if (s.getTotalBoarding() > avgPax * 2) {
                recs.add(StationAnalyticsResponse.Recommendation.builder()
                        .stationName(s.getStationName())
                        .passengers(s.getTotalBoarding())
                        .pattern("Consistently high boarding demand")
                        .suggestedAction("Review bus availability and platform capacity")
                        .reason(String.format("%,d passengers vs avg %,d — %dx above average",
                                s.getTotalBoarding(), avgPax, s.getTotalBoarding() / Math.max(avgPax, 1)))
                        .severity("HIGH").build());
            } else if (s.getTotalBoarding() < avgPax / 3 && s.getTotalBoarding() > 0) {
                recs.add(StationAnalyticsResponse.Recommendation.builder()
                        .stationName(s.getStationName())
                        .passengers(s.getTotalBoarding())
                        .pattern("Consistently low boarding demand")
                        .suggestedAction("Investigate service utilisation and review route frequency")
                        .reason(String.format("Only %,d passengers recorded — well below average of %,d",
                                s.getTotalBoarding(), avgPax))
                        .severity("LOW").build());
            }

            routes.stream()
                    .filter(r -> r.getPctOfStation() > 60)
                    .findFirst()
                    .ifPresent(r -> recs.add(StationAnalyticsResponse.Recommendation.builder()
                            .stationName(s.getStationName())
                            .routeName(r.getRouteName())
                            .passengers(r.getPassengers())
                            .pattern("Single route dominates station demand")
                            .suggestedAction("Review bus frequency on " + r.getRouteName())
                            .reason(String.format("%.1f%% of boardings come from one route (%,d passengers)",
                                    r.getPctOfStation(), r.getPassengers()))
                            .severity("MEDIUM").build()));
        }
        return recs;
    }

    private String boardingDemandLevel(long boarding, long max) {
        if (max == 0) return "LOW";
        double pct = (double) boarding / max;
        if (pct >= 0.6) return "HIGH";
        if (pct >= 0.25) return "MEDIUM";
        return "LOW";
    }
}
