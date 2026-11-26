package com.ktroute.app.service;

import com.ktroute.app.dto.request.DriverRequest;
import com.ktroute.app.dto.response.DriverResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface DriverService {
    DriverResponse createDriver(DriverRequest request);
    DriverResponse updateDriver(Long id, DriverRequest request);
    DriverResponse getDriver(Long id);
    List<DriverResponse> getAllDrivers();
    List<DriverResponse> getAvailableDrivers(LocalDateTime start, LocalDateTime end);
    void deactivateDriver(Long id);
}
