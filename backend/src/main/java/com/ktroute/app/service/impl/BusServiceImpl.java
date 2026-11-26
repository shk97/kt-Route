package com.ktroute.app.service.impl;

import com.ktroute.app.dto.request.BusRequest;
import com.ktroute.app.dto.response.BusResponse;
import com.ktroute.app.entity.Bus;
import com.ktroute.app.entity.Seat;
import com.ktroute.app.enums.BusStatus;
import com.ktroute.app.enums.SeatType;
import com.ktroute.app.exception.DuplicateResourceException;
import com.ktroute.app.exception.ResourceNotFoundException;
import com.ktroute.app.repository.BusRepository;
import com.ktroute.app.repository.SeatRepository;
import com.ktroute.app.service.BusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional
    public BusResponse createBus(BusRequest req) {
        if (busRepository.existsByBusNumber(req.getBusNumber())) {
            throw new DuplicateResourceException("Bus number already exists: " + req.getBusNumber());
        }
        if (busRepository.existsByRegistrationNo(req.getRegistrationNo())) {
            throw new DuplicateResourceException("Registration number already exists: " + req.getRegistrationNo());
        }
        Bus bus = Bus.builder()
                .busNumber(req.getBusNumber())
                .registrationNo(req.getRegistrationNo())
                .busType(req.getBusType())
                .totalSeats(req.getTotalSeats())
                .status(req.getStatus() != null ? req.getStatus() : BusStatus.ACTIVE)
                .manufacturer(req.getManufacturer())
                .model(req.getModel())
                .manufacturingYear(req.getManufacturingYear())
                .build();
        bus = busRepository.save(bus);
        generateSeats(bus);
        return mapToResponse(bus);
    }

    private void generateSeats(Bus bus) {
        List<Seat> seats = new ArrayList<>();
        int total = bus.getTotalSeats();
        for (int i = 1; i <= total; i++) {
            SeatType type = (i % 3 == 0) ? SeatType.AISLE : (i % 3 == 1 ? SeatType.WINDOW : SeatType.MIDDLE);
            seats.add(Seat.builder()
                    .bus(bus)
                    .seatNumber(String.format("%02d", i))
                    .seatType(type)
                    .isActive(true)
                    .build());
        }
        seatRepository.saveAll(seats);
    }

    @Override
    @Transactional
    public BusResponse updateBus(Long id, BusRequest req) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id));
        bus.setBusType(req.getBusType());
        if (req.getStatus() != null) bus.setStatus(req.getStatus());
        bus.setManufacturer(req.getManufacturer());
        bus.setModel(req.getModel());
        bus.setManufacturingYear(req.getManufacturingYear());
        return mapToResponse(busRepository.save(bus));
    }

    @Override
    public BusResponse getBus(Long id) {
        return mapToResponse(busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id)));
    }

    @Override
    public List<BusResponse> getAllBuses() {
        return busRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<BusResponse> getActiveBuses() {
        return busRepository.findByStatus(BusStatus.ACTIVE).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<BusResponse> getAvailableBuses(LocalDateTime start, LocalDateTime end) {
        return busRepository.findAvailableBuses(start, end).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateBus(Long id) {
        Bus bus = busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", "id", id));
        bus.setStatus(BusStatus.INACTIVE);
        busRepository.save(bus);
    }

    public BusResponse mapToResponse(Bus b) {
        BusResponse res = new BusResponse();
        res.setId(b.getId());
        res.setBusNumber(b.getBusNumber());
        res.setRegistrationNo(b.getRegistrationNo());
        res.setBusType(b.getBusType());
        res.setTotalSeats(b.getTotalSeats());
        res.setStatus(b.getStatus());
        res.setManufacturer(b.getManufacturer());
        res.setModel(b.getModel());
        res.setManufacturingYear(b.getManufacturingYear());
        res.setCreatedAt(b.getCreatedAt());
        return res;
    }
}
