package com.ktroute.app.dto.request;

import com.ktroute.app.enums.BusStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BusRequest {
    @NotBlank @Size(min=2, max=20)
    private String busNumber;

    @NotBlank @Size(min=5, max=20)
    private String registrationNo;

    @NotBlank @Size(min=2, max=50)
    private String busType;

    @NotNull @Min(1) @Max(100)
    private Integer totalSeats;

    private BusStatus status;
    private String manufacturer;
    private String model;
    private Integer manufacturingYear;
}
