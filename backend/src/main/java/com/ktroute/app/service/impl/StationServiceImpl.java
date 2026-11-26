package com.ktroute.app.service.impl;

import com.ktroute.app.dto.request.StationRequest;
import com.ktroute.app.dto.response.StationResponse;
import com.ktroute.app.entity.Station;
import com.ktroute.app.exception.DuplicateResourceException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.StationRepository;
import com.ktroute.app.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;

    @Override
    @Transactional
    public StationResponse createStation(StationRequest req) {
        if (stationRepository.existsByCode(req.getCode().toUpperCase())) {
            throw new DuplicateResourceException("Station code already exists: " + req.getCode());
        }
        Station station = Station.builder()
                .name(req.getName())
                .code(req.getCode().toUpperCase())
                .location(req.getLocation())
                .city(req.getCity())
                .state(req.getState())
                .isActive(true)
                .build();
        return mapToResponse(stationRepository.save(station));
    }

    @Override
    @Transactional
    public StationResponse updateStation(Long id, StationRequest req) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station", "id", id));
        station.setName(req.getName());
        station.setLocation(req.getLocation());
        station.setCity(req.getCity());
        station.setState(req.getState());
        return mapToResponse(stationRepository.save(station));
    }

    @Override
    public StationResponse getStation(Long id) {
        return mapToResponse(stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station", "id", id)));
    }

    @Override
    public List<StationResponse> getAllStations() {
        return stationRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<StationResponse> getActiveStations() {
        return stationRepository.findByIsActiveTrue().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateStation(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station", "id", id));
        station.setIsActive(false);
        stationRepository.save(station);
    }

    public StationResponse mapToResponse(Station s) {
        StationResponse res = new StationResponse();
        res.setId(s.getId());
        res.setName(s.getName());
        res.setCode(s.getCode());
        res.setLocation(s.getLocation());
        res.setCity(s.getCity());
        res.setState(s.getState());
        res.setIsActive(s.getIsActive());
        res.setCreatedAt(s.getCreatedAt());
        return res;
    }
}
