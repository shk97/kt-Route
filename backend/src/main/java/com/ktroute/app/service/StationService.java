package com.ktroute.app.service;

import com.ktroute.app.dto.request.StationRequest;
import com.ktroute.app.dto.response.StationResponse;
import java.util.List;

public interface StationService {
    StationResponse createStation(StationRequest request);
    StationResponse updateStation(Long id, StationRequest request);
    StationResponse getStation(Long id);
    List<StationResponse> getAllStations();
    List<StationResponse> getActiveStations();
    void deactivateStation(Long id);
}
