package com.ktroute.app.service;

import com.ktroute.app.dto.request.StationRequest;
import com.ktroute.app.dto.response.StationResponse;
import com.ktroute.app.entity.Station;
import com.ktroute.app.exception.DuplicateResourceException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.StationRepository;
import com.ktroute.app.service.impl.StationServiceImpl;
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
class StationServiceTest {

    @Mock private StationRepository stationRepository;

    @InjectMocks
    private StationServiceImpl stationService;

    private Station station;
    private StationRequest request;

    @BeforeEach
    void setUp() {
        station = Station.builder()
                .id(1L)
                .name("Central Station")
                .code("CST")
                .location("Main Road")
                .city("Mumbai")
                .state("Maharashtra")
                .isActive(true)
                .build();

        request = new StationRequest();
        request.setName("Central Station");
        request.setCode("cst");
        request.setLocation("Main Road");
        request.setCity("Mumbai");
        request.setState("Maharashtra");
    }

    @Test
    void createStation_success() {
        when(stationRepository.existsByCode("CST")).thenReturn(false);
        when(stationRepository.save(any(Station.class))).thenReturn(station);

        StationResponse response = stationService.createStation(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Central Station");
        assertThat(response.getCode()).isEqualTo("CST");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void createStation_duplicateCode_throwsException() {
        when(stationRepository.existsByCode("CST")).thenReturn(true);

        assertThatThrownBy(() -> stationService.createStation(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("cst");
    }

    @Test
    void getStation_success() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));

        StationResponse response = stationService.getStation(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Central Station");
    }

    @Test
    void getStation_notFound_throwsException() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.getStation(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllStations_returnsList() {
        when(stationRepository.findAll()).thenReturn(List.of(station));

        List<StationResponse> result = stationService.getAllStations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("CST");
    }

    @Test
    void getActiveStations_returnsOnlyActive() {
        when(stationRepository.findByIsActiveTrue()).thenReturn(List.of(station));

        List<StationResponse> result = stationService.getActiveStations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    void updateStation_success() {
        request.setName("Updated Station");
        request.setCity("Delhi");
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        StationResponse response = stationService.updateStation(1L, request);

        assertThat(response.getName()).isEqualTo("Updated Station");
        assertThat(response.getCity()).isEqualTo("Delhi");
    }

    @Test
    void updateStation_notFound_throwsException() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.updateStation(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivateStation_success() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        stationService.deactivateStation(1L);

        assertThat(station.getIsActive()).isFalse();
        verify(stationRepository).save(station);
    }

    @Test
    void deactivateStation_notFound_throwsException() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationService.deactivateStation(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
