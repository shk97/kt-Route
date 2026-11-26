package com.ktroute.app.dto.response;

import com.ktroute.app.enums.BusStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BusResponse {
    private Long id;
    private String busNumber;
    private String registrationNo;
    private String busType;
    private Integer totalSeats;
    private BusStatus status;
    private String manufacturer;
    private String model;
    private Integer manufacturingYear;
    private LocalDateTime createdAt;
}
