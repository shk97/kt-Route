package com.ktroute.app.service;

import com.ktroute.app.dto.request.TripRequest;
import com.ktroute.app.dto.response.TripResponse;
import java.time.LocalDate;
import java.util.List;

public interface TripService {
    TripResponse createTrip(TripRequest request, String adminEmail);
    TripResponse updateTrip(Long id, TripRequest request, String adminEmail);
    TripResponse getTrip(Long id);
    List<TripResponse> getAllTrips();
    List<TripResponse> searchTrips(Long startStationId, Long endStationId, LocalDate date);
    void cancelTrip(Long id, String reason, String adminEmail);
    List<TripResponse> getTripsByRoute(Long routeId);
}
