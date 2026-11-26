package com.ktroute.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StationRequest {
    @NotBlank @Size(min=2, max=150)
    private String name;

    @NotBlank @Size(min=2, max=10)
    private String code;

    @Size(max=255)
    private String location;

    @Size(max=100)
    private String city;

    @Size(max=100)
    private String state;
}
