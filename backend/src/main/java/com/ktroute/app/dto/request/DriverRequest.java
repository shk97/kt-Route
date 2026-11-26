package com.ktroute.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DriverRequest {
    @NotBlank @Size(min=2, max=100)
    private String fullName;

    @NotBlank @Size(min=5, max=30)
    private String licenseNo;

    @NotBlank @Pattern(regexp = "^[0-9]{10,15}$")
    private String phone;

    @Email
    private String email;
}
