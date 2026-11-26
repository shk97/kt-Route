package com.ktroute.app.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StationResponse {
    private Long id;
    private String name;
    private String code;
    private String location;
    private String city;
    private String state;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
