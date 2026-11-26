package com.ktroute.app.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Boolean isActive;
    private Set<String> roles;
    private LocalDateTime createdAt;
}
