package com.ktroute.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class BookingRequest {
    @NotNull
    private Long tripId;

    @NotNull
    private Long boardingStationId;

    @NotNull
    private Long alightingStationId;

    @NotNull @Size(min=1, max=10)
    @Valid
    private List<PassengerDetail> passengers;

    @Data
    public static class PassengerDetail {
        @NotBlank @Size(min=2, max=100)
        private String passengerName;

        @NotNull @Min(1) @Max(120)
        private Integer age;

        @NotBlank
        private String seatNumber;
    }
}
