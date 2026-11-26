package com.ktroute.app.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DriverResponse {
    private Long id;
    private String fullName;
    private String licenseNo;
    private String phone;
    private String email;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
