package com.ktroute.app.service;

import com.ktroute.app.dto.request.DriverRequest;
import com.ktroute.app.dto.response.DriverResponse;
import com.ktroute.app.entity.Driver;
import com.ktroute.app.exception.DuplicateResourceException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.DriverRepository;
import com.ktroute.app.service.impl.DriverServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock private DriverRepository driverRepository;

    @InjectMocks
    private DriverServiceImpl driverService;

    private Driver driver;
    private DriverRequest request;

    @BeforeEach
    void setUp() {
        driver = Driver.builder()
                .id(1L)
                .fullName("John Doe")
                .licenseNo("LIC-001")
                .phone("9876543210")
                .email("john@example.com")
                .isActive(true)
                .build();

        request = new DriverRequest();
        request.setFullName("John Doe");
        request.setLicenseNo("LIC-001");
        request.setPhone("9876543210");
        request.setEmail("john@example.com");
    }

    @Test
    void createDriver_success() {
        when(driverRepository.existsByLicenseNo("LIC-001")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        DriverResponse response = driverService.createDriver(request);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getLicenseNo()).isEqualTo("LIC-001");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void createDriver_duplicateLicense_throwsException() {
        when(driverRepository.existsByLicenseNo("LIC-001")).thenReturn(true);

        assertThatThrownBy(() -> driverService.createDriver(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("LIC-001");
    }

    @Test
    void getDriver_success() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));

        DriverResponse response = driverService.getDriver(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("John Doe");
    }

    @Test
    void getDriver_notFound_throwsException() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getDriver(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllDrivers_returnsList() {
        when(driverRepository.findAll()).thenReturn(List.of(driver));

        List<DriverResponse> result = driverService.getAllDrivers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLicenseNo()).isEqualTo("LIC-001");
    }

    @Test
    void updateDriver_success() {
        request.setFullName("Jane Doe");
        request.setPhone("1234567890");
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.updateDriver(1L, request);

        assertThat(response.getFullName()).isEqualTo("Jane Doe");
        assertThat(response.getPhone()).isEqualTo("1234567890");
    }

    @Test
    void updateDriver_notFound_throwsException() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.updateDriver(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivateDriver_success() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        driverService.deactivateDriver(1L);

        assertThat(driver.getIsActive()).isFalse();
        verify(driverRepository).save(driver);
    }

    @Test
    void deactivateDriver_notFound_throwsException() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.deactivateDriver(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
