package com.ktroute.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String  token;
    @Builder.Default
    private String  type = "Bearer";
    private Long    id;
    private String  email;
    private String  fullName;
    private Set<String> roles;
}
