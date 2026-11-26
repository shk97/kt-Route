package com.ktroute.app.service.impl;

import com.ktroute.app.dto.request.DriverRequest;
import com.ktroute.app.dto.response.DriverResponse;
import com.ktroute.app.entity.Driver;
import com.ktroute.app.exception.DuplicateResourceException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.DriverRepository;
import com.ktroute.app.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    @Override
    @Transactional
    public DriverResponse createDriver(DriverRequest req) {
        if (driverRepository.existsByLicenseNo(req.getLicenseNo())) {
            throw new DuplicateResourceException("License number already registered: " + req.getLicenseNo());
        }
        Driver driver = Driver.builder()
                .fullName(req.getFullName())
                .licenseNo(req.getLicenseNo())
                .phone(req.getPhone())
                .email(req.getEmail())
                .isActive(true)
                .build();
        return mapToResponse(driverRepository.save(driver));
    }

    @Override
    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest req) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", id));
        driver.setFullName(req.getFullName());
        driver.setPhone(req.getPhone());
        driver.setEmail(req.getEmail());
        return mapToResponse(driverRepository.save(driver));
    }

    @Override
    public DriverResponse getDriver(Long id) {
        return mapToResponse(driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", id)));
    }

    @Override
    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<DriverResponse> getAvailableDrivers(LocalDateTime start, LocalDateTime end) {
        return driverRepository.findAvailableDrivers(start, end).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", id));
        driver.setIsActive(false);
        driverRepository.save(driver);
    }

    public DriverResponse mapToResponse(Driver d) {
        DriverResponse res = new DriverResponse();
        res.setId(d.getId());
        res.setFullName(d.getFullName());
        res.setLicenseNo(d.getLicenseNo());
        res.setPhone(d.getPhone());
        res.setEmail(d.getEmail());
        res.setIsActive(d.getIsActive());
        res.setCreatedAt(d.getCreatedAt());
        return res;
    }
}
