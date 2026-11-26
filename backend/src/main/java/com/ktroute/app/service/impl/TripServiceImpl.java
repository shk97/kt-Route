package com.ktroute.app.service.impl;

import com.ktroute.app.dto.request.TripRequest;
import com.ktroute.app.dto.response.BusResponse;
import com.ktroute.app.dto.response.DriverResponse;
import com.ktroute.app.dto.response.RouteResponse;
import com.ktroute.app.dto.response.TripResponse;
import com.ktroute.app.entity.*;
import com.ktroute.app.enums.ChangeType;
import com.ktroute.app.enums.TripStatus;
import com.ktroute.app.exception.AppException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.*;
import com.ktroute.app.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ScheduleChangeHistoryRepository changeHistoryRepository;

    @Override
    @Transactional
    public TripResponse createTrip(TripRequest req, String adminEmail) {
        if (req.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new AppException("Trip departure time must be in the future", HttpStatus.BAD_REQUEST);
        }
        validateSchedule(req, null);
        Route route = routeRepository.findById(req.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", req.getRouteId()));
        Bus bus = busRepository.findById(req.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", req.getBusId()));
        Driver driver = req.getDriverId() != null ?
                driverRepository.findById(req.getDriverId()).orElse(null) : null;

        Trip trip = Trip.builder()
                .route(route)
                .bus(bus)
                .driver(driver)
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .basePrice(req.getBasePrice())
                .status(TripStatus.SCHEDULED)
                .notes(req.getNotes())
                .build();
        trip = tripRepository.save(trip);

        recordChange(trip, adminEmail, ChangeType.CREATED, "Trip created", req.getChangeReason());
        return mapToResponse(trip);
    }

    @Override
    @Transactional
    public TripResponse updateTrip(Long id, TripRequest req, String adminEmail) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", id));
        validateSchedule(req, id);

        Bus bus = busRepository.findById(req.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", req.getBusId()));
        Driver driver = req.getDriverId() != null ?
                driverRepository.findById(req.getDriverId()).orElse(null) : null;

        StringBuilder desc = new StringBuilder();
        if (!trip.getBus().getId().equals(req.getBusId())) desc.append("Bus changed. ");
        if (!trip.getDepartureTime().equals(req.getDepartureTime())) desc.append("Departure time changed. ");

        trip.setBus(bus);
        trip.setDriver(driver);
        trip.setDepartureTime(req.getDepartureTime());
        trip.setArrivalTime(req.getArrivalTime());
        trip.setBasePrice(req.getBasePrice());
        trip.setNotes(req.getNotes());

        trip = tripRepository.save(trip);
        recordChange(trip, adminEmail, ChangeType.TIME_CHANGED, desc.toString(), req.getChangeReason());
        return mapToResponse(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getTrip(Long id) {
        return mapToResponse(tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getAllTrips() {
        List<Trip> trips = tripRepository.findAll();
        if (trips.isEmpty()) return List.of();
        List<Long> ids = trips.stream().map(Trip::getId).collect(Collectors.toList());
        Map<Long, Integer> countMap = bookingRepository.countConfirmedPassengersForTripIds(ids)
                .stream().collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).intValue()
                ));
        return trips.stream()
                .map(t -> mapToResponse(t, countMap.getOrDefault(t.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> searchTrips(Long startStationId, Long endStationId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        var trips = tripRepository.searchTrips(startStationId, endStationId, from, to);
        return trips.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelTrip(Long id, String reason, String adminEmail) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", id));
        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new AppException("Cannot cancel a completed trip", HttpStatus.BAD_REQUEST);
        }
        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
        recordChange(trip, adminEmail, ChangeType.CANCELLED, "Trip cancelled", reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByRoute(Long routeId) {
        return tripRepository.findByRouteIdOrderByDepartureTime(routeId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private void validateSchedule(TripRequest req, Long excludeTripId) {
        if (req.getArrivalTime().isBefore(req.getDepartureTime()) || req.getArrivalTime().equals(req.getDepartureTime())) {
            throw new AppException("Arrival time must be after departure time", HttpStatus.BAD_REQUEST);
        }
        List<Trip> busConflicts = tripRepository.findConflictingBusTrips(req.getBusId(), req.getDepartureTime(), req.getArrivalTime());
        if (excludeTripId != null) busConflicts = busConflicts.stream().filter(t -> !t.getId().equals(excludeTripId)).collect(Collectors.toList());
        if (!busConflicts.isEmpty()) throw new AppException("Bus already scheduled during this time", HttpStatus.CONFLICT);

        if (req.getDriverId() != null) {
            List<Trip> driverConflicts = tripRepository.findConflictingDriverTrips(req.getDriverId(), req.getDepartureTime(), req.getArrivalTime());
            if (excludeTripId != null) driverConflicts = driverConflicts.stream().filter(t -> !t.getId().equals(excludeTripId)).collect(Collectors.toList());
            if (!driverConflicts.isEmpty()) throw new AppException("Driver already scheduled during this time", HttpStatus.CONFLICT);
        }
    }

    private void recordChange(Trip trip, String adminEmail, ChangeType type, String desc, String reason) {
        userRepository.findByEmail(adminEmail).ifPresent(admin -> {
            ScheduleChangeHistory history = ScheduleChangeHistory.builder()
                    .trip(trip)
                    .changedBy(admin)
                    .changeType(type)
                    .changeDescription(desc)
                    .reason(reason)
                    .build();
            changeHistoryRepository.save(history);
        });
    }

    public TripResponse mapToResponse(Trip t) {
        Integer confirmed = bookingRepository.countConfirmedPassengersForTrip(t.getId());
        return mapToResponse(t, confirmed != null ? confirmed : 0);
    }

    public TripResponse mapToResponse(Trip t, int confirmedPassengers) {
        TripResponse res = new TripResponse();
        res.setId(t.getId());
        res.setDepartureTime(t.getDepartureTime());
        res.setArrivalTime(t.getArrivalTime());
        res.setBasePrice(t.getBasePrice());
        res.setStatus(t.getStatus());
        res.setNotes(t.getNotes());
        res.setCreatedAt(t.getCreatedAt());

        if (t.getRoute() != null) {
            res.setRouteName(t.getRoute().getName());
            res.setRouteId(t.getRoute().getId());
            RouteResponse routeRes = new RouteResponse();
            routeRes.setId(t.getRoute().getId());
            routeRes.setName(t.getRoute().getName());
            routeRes.setRouteCode(t.getRoute().getRouteCode());
            routeRes.setDistanceKm(t.getRoute().getDistanceKm());
            routeRes.setEstimatedDurationMinutes(t.getRoute().getEstimatedDurationMinutes());
            routeRes.setStatus(t.getRoute().getStatus());
            if (t.getRoute().getStartStation() != null) {
                RouteResponse.StationSummary ss = toStationSummary(t.getRoute().getStartStation());
                res.setStartStation(ss);
                routeRes.setStartStation(ss);
            }
            if (t.getRoute().getEndStation() != null) {
                RouteResponse.StationSummary es = toStationSummary(t.getRoute().getEndStation());
                res.setEndStation(es);
                routeRes.setEndStation(es);
            }
            if (t.getRoute().getStops() != null) {
                routeRes.setStops(t.getRoute().getStops().stream().map(stop -> {
                    RouteResponse.RouteStopResponse sr = new RouteResponse.RouteStopResponse();
                    sr.setId(stop.getId());
                    sr.setStopOrder(stop.getStopOrder());
                    sr.setArrivalOffsetMinutes(stop.getArrivalOffsetMinutes());
                    sr.setDepartureOffsetMinutes(stop.getDepartureOffsetMinutes());
                    sr.setDistanceFromStartKm(stop.getDistanceFromStartKm());
                    if (stop.getStation() != null) {
                        sr.setStation(toStationSummary(stop.getStation()));
                    }
                    return sr;
                }).collect(Collectors.toList()));
            }
            res.setRoute(routeRes);
        }
        if (t.getBus() != null) {
            BusResponse br = new BusResponse();
            br.setId(t.getBus().getId());
            br.setBusNumber(t.getBus().getBusNumber());
            br.setBusType(t.getBus().getBusType());
            br.setTotalSeats(t.getBus().getTotalSeats());
            br.setStatus(t.getBus().getStatus());
            res.setBus(br);
            res.setTotalSeats(t.getBus().getTotalSeats());
        }
        if (t.getDriver() != null) {
            DriverResponse dr = new DriverResponse();
            dr.setId(t.getDriver().getId());
            dr.setFullName(t.getDriver().getFullName());
            dr.setPhone(t.getDriver().getPhone());
            res.setDriver(dr);
        }
        res.setConfirmedPassengers(confirmedPassengers);
        if (t.getBus() != null) {
            res.setAvailableSeats(t.getBus().getTotalSeats() - confirmedPassengers);
        }
        return res;
    }

    private static RouteResponse.StationSummary toStationSummary(Station s) {
        RouteResponse.StationSummary ss = new RouteResponse.StationSummary();
        ss.setId(s.getId());
        ss.setName(s.getName());
        ss.setCode(s.getCode());
        ss.setCity(s.getCity());
        return ss;
    }
}
