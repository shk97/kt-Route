package com.ktroute.app.service;

import com.ktroute.app.dto.request.BusRequest;
import com.ktroute.app.dto.response.BusResponse;
import com.ktroute.app.entity.Bus;
import com.ktroute.app.enums.BusStatus;
import com.ktroute.app.exception.DuplicateResourceException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.BusRepository;
import com.ktroute.app.repository.SeatRepository;
import com.ktroute.app.service.impl.BusServiceImpl;
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
class BusServiceTest {

    @Mock private BusRepository busRepository;
    @Mock private SeatRepository seatRepository;

    @InjectMocks
    private BusServiceImpl busService;

    private Bus bus;
    private BusRequest request;

    @BeforeEach
    void setUp() {
        bus = Bus.builder()
                .id(1L)
                .busNumber("BUS-001")
                .registrationNo("REG-001")
                .busType("AC")
                .totalSeats(40)
                .status(BusStatus.ACTIVE)
                .manufacturer("Tata")
                .model("Starbus")
                .manufacturingYear(2022)
                .build();

        request = new BusRequest();
        request.setBusNumber("BUS-001");
        request.setRegistrationNo("REG-001");
        request.setBusType("AC");
        request.setTotalSeats(40);
        request.setManufacturer("Tata");
        request.setModel("Starbus");
        request.setManufacturingYear(2022);
    }

    @Test
    void createBus_success() {
        when(busRepository.existsByBusNumber("BUS-001")).thenReturn(false);
        when(busRepository.existsByRegistrationNo("REG-001")).thenReturn(false);
        when(busRepository.save(any(Bus.class))).thenReturn(bus);

        BusResponse response = busService.createBus(request);

        assertThat(response).isNotNull();
        assertThat(response.getBusNumber()).isEqualTo("BUS-001");
        assertThat(response.getStatus()).isEqualTo(BusStatus.ACTIVE);
        verify(seatRepository).saveAll(anyList());
    }

    @Test
    void createBus_duplicateBusNumber_throwsException() {
        when(busRepository.existsByBusNumber("BUS-001")).thenReturn(true);

        assertThatThrownBy(() -> busService.createBus(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("BUS-001");
    }

    @Test
    void createBus_duplicateRegistrationNo_throwsException() {
        when(busRepository.existsByBusNumber("BUS-001")).thenReturn(false);
        when(busRepository.existsByRegistrationNo("REG-001")).thenReturn(true);

        assertThatThrownBy(() -> busService.createBus(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("REG-001");
    }

    @Test
    void getBus_success() {
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));

        BusResponse response = busService.getBus(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBusNumber()).isEqualTo("BUS-001");
    }

    @Test
    void getBus_notFound_throwsException() {
        when(busRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> busService.getBus(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllBuses_returnsList() {
        when(busRepository.findAll()).thenReturn(List.of(bus));

        List<BusResponse> result = busService.getAllBuses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBusNumber()).isEqualTo("BUS-001");
    }

    @Test
    void updateBus_success() {
        request.setBusType("Non-AC");
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(busRepository.save(any(Bus.class))).thenAnswer(inv -> inv.getArgument(0));

        BusResponse response = busService.updateBus(1L, request);

        assertThat(response.getBusType()).isEqualTo("Non-AC");
    }

    @Test
    void updateBus_notFound_throwsException() {
        when(busRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> busService.updateBus(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivateBus_success() {
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(busRepository.save(any(Bus.class))).thenAnswer(inv -> inv.getArgument(0));

        busService.deactivateBus(1L);

        assertThat(bus.getStatus()).isEqualTo(BusStatus.INACTIVE);
        verify(busRepository).save(bus);
    }

    @Test
    void deactivateBus_notFound_throwsException() {
        when(busRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> busService.deactivateBus(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
