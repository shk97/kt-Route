package com.ktroute.app.service;

import com.ktroute.app.dto.request.BusRequest;
import com.ktroute.app.dto.response.BusResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface BusService {
    BusResponse createBus(BusRequest request);
    BusResponse updateBus(Long id, BusRequest request);
    BusResponse getBus(Long id);
    List<BusResponse> getAllBuses();
    List<BusResponse> getActiveBuses();
    List<BusResponse> getAvailableBuses(LocalDateTime start, LocalDateTime end);
    void deactivateBus(Long id);
}
