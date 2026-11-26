package com.ktroute.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktroute.app.dto.request.LoginRequest;
import com.ktroute.app.dto.request.RegisterRequest;
import com.ktroute.app.dto.response.AuthResponse;
import com.ktroute.app.security.UserDetailsServiceImpl;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.AuthService;
import com.ktroute.app.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private TokenRevocationService tokenRevocationService;

    @Test
    void register_success_returns200() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .email("user@test.com")
                .fullName("Test User")
                .token("mock.jwt.token")
                .roles(Set.of("ROLE_PASSENGER"))
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test User");
        req.setEmail("user@test.com");
        req.setPhone("9876543210");
        req.setPassword("Test@1234");
        req.setRole("PASSENGER");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test User");
        req.setEmail("not-an-email");
        req.setPhone("9876543210");
        req.setPassword("Test@1234");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test User");
        req.setEmail("user@test.com");
        req.setPhone("9876543210");
        req.setPassword("weakpassword");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_success_returns200() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .email("user@test.com")
                .token("mock.jwt.token")
                .roles(Set.of("ROLE_PASSENGER"))
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("Test@1234");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"));
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        // password missing

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
