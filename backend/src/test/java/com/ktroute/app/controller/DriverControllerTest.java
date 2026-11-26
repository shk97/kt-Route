package com.ktroute.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktroute.app.dto.request.DriverRequest;
import com.ktroute.app.dto.response.DriverResponse;
import com.ktroute.app.security.UserDetailsServiceImpl;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.DriverService;
import com.ktroute.app.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(DriverController.class)
@ActiveProfiles("test")
class DriverControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private DriverService driverService;
    @MockBean  private JwtUtils jwtUtils;
    @MockBean  private UserDetailsServiceImpl userDetailsService;
    @MockBean  private TokenRevocationService tokenRevocationService;

    // ── GET /api/admin/drivers ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllDrivers_admin_returns200() throws Exception {
        when(driverService.getAllDrivers()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/admin/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fullName").value("John Doe"));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void getAllDrivers_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/drivers"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllDrivers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/drivers"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/admin/drivers/{id} ───────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDriver_admin_returns200() throws Exception {
        when(driverService.getDriver(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/admin/drivers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.licenseNo").value("LIC-001"));
    }

    // ── POST /api/admin/drivers ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDriver_admin_returns200() throws Exception {
        DriverRequest req = buildRequest();
        when(driverService.createDriver(any(DriverRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/admin/drivers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("John Doe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDriver_invalidPhone_returns400() throws Exception {
        DriverRequest req = buildRequest();
        req.setPhone("not-a-phone");

        mockMvc.perform(post("/api/admin/drivers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/admin/drivers/{id} ───────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateDriver_admin_returns200() throws Exception {
        DriverRequest req = buildRequest();
        when(driverService.updateDriver(eq(1L), any(DriverRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(put("/api/admin/drivers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ── DELETE /api/admin/drivers/{id} ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateDriver_admin_returns200() throws Exception {
        doNothing().when(driverService).deactivateDriver(1L);

        mockMvc.perform(delete("/api/admin/drivers/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DriverRequest buildRequest() {
        DriverRequest req = new DriverRequest();
        req.setFullName("John Doe");
        req.setLicenseNo("LIC-001");
        req.setPhone("9876543210");
        req.setEmail("john@example.com");
        return req;
    }

    private DriverResponse buildResponse() {
        DriverResponse res = new DriverResponse();
        res.setId(1L);
        res.setFullName("John Doe");
        res.setLicenseNo("LIC-001");
        res.setPhone("9876543210");
        res.setEmail("john@example.com");
        res.setIsActive(true);
        return res;
    }
}
