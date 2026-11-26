package com.ktroute.app;

import com.ktroute.app.entity.*;
import com.ktroute.app.enums.*;
import com.ktroute.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class MockDataSeeder implements CommandLineRunner {

    private final StationRepository     stationRepository;
    private final RouteRepository       routeRepository;
    private final RouteStopRepository   routeStopRepository;
    private final BusRepository         busRepository;
    private final TripRepository        tripRepository;
    private final BookingRepository     bookingRepository;
    private final BoardingRecordRepository boardingRecordRepository;
    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JdbcTemplate          jdbcTemplate;

    private final Random rng = new Random(42);

    // ── Station specs ────────────────────────────────────────────────────────────

    private record StationSpec(String code, String name, String city, String state, double lat, double lon) {}

    private static final List<StationSpec> STATION_SPECS = List.of(
        new StationSpec("CHN", "Chennai Central Bus Terminus", "Chennai",      "Tamil Nadu",  13.0827, 80.2707),
        new StationSpec("CBE", "Coimbatore Central Bus Stand", "Coimbatore",   "Tamil Nadu",  11.0168, 76.9558),
        new StationSpec("MDU", "Madurai Mattuthavani Bus Stand","Madurai",     "Tamil Nadu",   9.9252, 78.1198),
        new StationSpec("TRY", "Trichy Central Bus Station",    "Tiruchirappalli","Tamil Nadu",10.7905, 78.7047),
        new StationSpec("SLM", "Salem New Bus Stand",           "Salem",       "Tamil Nadu",  11.6643, 78.1460),
        new StationSpec("TNV", "Tirunelveli Bus Terminus",      "Tirunelveli", "Tamil Nadu",   8.7139, 77.7567),
        new StationSpec("VLR", "Vellore Katpadi Bus Stand",     "Vellore",     "Tamil Nadu",  12.9165, 79.1325),
        new StationSpec("ERD", "Erode Bus Stand",               "Erode",       "Tamil Nadu",  11.3410, 77.7172)
    );

    // ── Route specs ──────────────────────────────────────────────────────────────

    private record RouteSpec(
        String code, String name,
        String startCode, String endCode,
        double distKm, int durationMin,
        String[] intermediateCodes,
        // boarding-weight per boarding station (parallel to [start, ...intermediates])
        int[] boardingWeights
    ) {}

    private static final List<RouteSpec> ROUTE_SPECS = List.of(
        new RouteSpec("RT-CHN-CBE", "Chennai – Coimbatore Express",
            "CHN", "CBE", 505, 480,
            new String[]{"VLR", "SLM", "ERD"},
            new int[]{50, 20, 16, 14}),       // CHN=50%, VLR=20%, SLM=16%, ERD=14%

        new RouteSpec("RT-CHN-MDU", "Chennai – Madurai Express",
            "CHN", "MDU", 461, 450,
            new String[]{"TRY"},
            new int[]{62, 38}),               // CHN=62%, TRY=38%

        new RouteSpec("RT-CBE-MDU", "Coimbatore – Madurai",
            "CBE", "MDU", 221, 240,
            new String[]{"ERD", "TRY"},
            new int[]{58, 24, 18}),           // CBE=58%, ERD=24%, TRY=18%

        new RouteSpec("RT-CHN-TNV", "Chennai – Tirunelveli",
            "CHN", "TNV", 636, 600,
            new String[]{"MDU"},
            new int[]{68, 32}),               // CHN=68%, MDU=32%

        new RouteSpec("RT-VLR-CBE", "Vellore – Coimbatore",
            "VLR", "CBE", 283, 300,
            new String[]{"SLM", "ERD"},
            new int[]{52, 30, 18})            // VLR=52%, SLM=30%, ERD=18%
    );

    // ── Bus specs ────────────────────────────────────────────────────────────────

    private record BusSpec(String num, String reg, String type, int seats) {}

    private static final List<BusSpec> BUS_SPECS = List.of(
        new BusSpec("KT-001", "TN01AB1001", "AC Sleeper",      40),
        new BusSpec("KT-002", "TN01AB1002", "AC Semi-Sleeper", 45),
        new BusSpec("KT-003", "TN01AB1003", "Non-AC",          50)
    );

    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void run(String... args) {
        if (boardingRecordRepository.count() > 0) {
            log.info("Mock boarding data already present — skipping");
            return;
        }

        log.info("Seeding mock station boarding data...");

        Map<String, Station> stMap = seedStations();
        List<Bus>  buses     = seedBuses();
        User       passenger = seedPassengerUser();

        int totalRecords = 0;
        for (RouteSpec rs : ROUTE_SPECS) {
            Route route = seedRoute(rs, stMap);
            totalRecords += generateTripsAndBoardings(route, rs, stMap, buses, passenger);
        }

        log.info("Mock data seeding complete — {} boarding records inserted", totalRecords);
    }

    // ── Seed helpers ─────────────────────────────────────────────────────────────

    private Map<String, Station> seedStations() {
        Map<String, Station> map = new LinkedHashMap<>();
        for (StationSpec s : STATION_SPECS) {
            Station st = stationRepository.findByCode(s.code()).orElseGet(() ->
                stationRepository.save(Station.builder()
                    .name(s.name()).code(s.code())
                    .city(s.city()).state(s.state())
                    .latitude(s.lat()).longitude(s.lon())
                    .location(s.city() + ", " + s.state())
                    .isActive(true).build())
            );
            map.put(s.code(), st);
        }
        log.info("Stations ready: {}", map.size());
        return map;
    }

    private List<Bus> seedBuses() {
        List<Bus> buses = new ArrayList<>();
        for (BusSpec b : BUS_SPECS) {
            Bus bus = busRepository.findByBusNumber(b.num()).orElseGet(() ->
                busRepository.save(Bus.builder()
                    .busNumber(b.num()).registrationNo(b.reg())
                    .busType(b.type()).totalSeats(b.seats())
                    .status(BusStatus.ACTIVE).build())
            );
            buses.add(bus);
        }
        return buses;
    }

    private User seedPassengerUser() {
        String email = "mock.passenger@ktroute.com";
        return userRepository.findByEmail(email).orElseGet(() -> {
            Role passengerRole = roleRepository.findByName(RoleName.ROLE_PASSENGER)
                .orElseThrow(() -> new RuntimeException("ROLE_PASSENGER not found"));
            return userRepository.save(User.builder()
                .fullName("Mock Passenger").email(email).phone("9000000001")
                .password(passwordEncoder.encode("MockPass@1"))
                .roles(Set.of(passengerRole)).isActive(true).build());
        });
    }

    private Route seedRoute(RouteSpec rs, Map<String, Station> stMap) {
        return routeRepository.findByRouteCode(rs.code()).orElseGet(() -> {
            Station start = stMap.get(rs.startCode());
            Station end   = stMap.get(rs.endCode());

            Route route = routeRepository.save(Route.builder()
                .routeCode(rs.code()).name(rs.name())
                .startStation(start).endStation(end)
                .distanceKm(rs.distKm())
                .estimatedDurationMinutes(rs.durationMin())
                .status(RouteStatus.ACTIVE).build());

            // stop order: start=1, intermediates=2..n, end=n+1
            saveStop(route, start, 1, 0, 0, 0.0);
            int stopIdx = 2;
            double[] interOffsets = intermediateKmOffsets(rs.distKm(), rs.intermediateCodes().length);
            for (int i = 0; i < rs.intermediateCodes().length; i++) {
                Station inter = stMap.get(rs.intermediateCodes()[i]);
                int arrMin = (int)(rs.durationMin() * interOffsets[i]);
                saveStop(route, inter, stopIdx++, arrMin, arrMin + 5, interOffsets[i] * rs.distKm());
            }
            saveStop(route, end, stopIdx, rs.durationMin(), rs.durationMin(), rs.distKm());
            return route;
        });
    }

    private void saveStop(Route route, Station station, int order, int arrOff, int depOff, double distKm) {
        routeStopRepository.save(RouteStop.builder()
            .route(route).station(station).stopOrder(order)
            .arrivalOffsetMinutes(arrOff).departureOffsetMinutes(depOff)
            .distanceFromStartKm(distKm).build());
    }

    private double[] intermediateKmOffsets(double totalDist, int count) {
        double[] offsets = new double[count];
        for (int i = 0; i < count; i++) {
            offsets[i] = (double)(i + 1) / (count + 1);
        }
        return offsets;
    }

    // ── Trip / Boarding generation ────────────────────────────────────────────────

    private int generateTripsAndBoardings(Route route, RouteSpec rs,
                                           Map<String, Station> stMap,
                                           List<Bus> buses, User passenger) {
        int totalInserted = 0;

        // All boarding stations for this route (start + intermediates)
        List<Station> boardingStations = new ArrayList<>();
        boardingStations.add(stMap.get(rs.startCode()));
        for (String c : rs.intermediateCodes()) boardingStations.add(stMap.get(c));

        Station alightStation = stMap.get(rs.endCode());

        // 2 trips per month for the past 12 months
        LocalDateTime base = LocalDateTime.now().minusMonths(12);
        for (int month = 0; month < 12; month++) {
            for (int t = 0; t < 2; t++) {
                LocalDateTime dep = base.plusMonths(month)
                    .withDayOfMonth(t == 0 ? 5 : 20)
                    .withHour(t == 0 ? 6 : 20).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime arr = dep.plusMinutes(rs.durationMin());

                Bus bus = buses.get((month * 2 + t) % buses.size());
                BigDecimal price = BigDecimal.valueOf(rng.nextInt(300) + 200L);

                Trip trip = tripRepository.save(Trip.builder()
                    .route(route).bus(bus)
                    .departureTime(dep).arrivalTime(arr)
                    .basePrice(price).status(TripStatus.COMPLETED).build());

                // 4–8 bookings per trip
                int numBookings = 4 + rng.nextInt(5);
                for (int b = 0; b < numBookings; b++) {
                    Station boardSt = pickWeighted(boardingStations, rs.boardingWeights());
                    int pax = 1 + rng.nextInt(2);
                    BigDecimal amount = price.multiply(BigDecimal.valueOf(pax));

                    Booking booking = bookingRepository.save(Booking.builder()
                        .user(passenger).trip(trip)
                        .passengerCount(pax).totalAmount(amount)
                        .status(BookingStatus.COMPLETED)
                        .boardingStation(boardSt).alightingStation(alightStation)
                        .build());

                    // Insert boarding record with historical recorded_at via JDBC
                    LocalDateTime recordedAt = dep.plusMinutes(rng.nextInt(30));
                    jdbcTemplate.update(
                        "INSERT INTO boarding_records (booking_id, trip_id, boarding_station_id, alighting_station_id, actual_boarding_time, passenger_count, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        booking.getId(), trip.getId(), boardSt.getId(), alightStation.getId(),
                        recordedAt, pax, recordedAt
                    );
                    totalInserted++;
                }
            }
        }
        return totalInserted;
    }

    private Station pickWeighted(List<Station> stations, int[] weights) {
        int total = Arrays.stream(weights).sum();
        int roll = rng.nextInt(total);
        int cum = 0;
        for (int i = 0; i < stations.size(); i++) {
            cum += weights[i];
            if (roll < cum) return stations.get(i);
        }
        return stations.get(stations.size() - 1);
    }
}
