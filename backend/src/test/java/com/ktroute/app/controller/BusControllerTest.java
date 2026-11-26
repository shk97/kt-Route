package com.ktroute.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktroute.app.dto.request.BusRequest;
import com.ktroute.app.dto.response.BusResponse;
import com.ktroute.app.enums.BusStatus;
import com.ktroute.app.security.UserDetailsServiceImpl;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.BusService;
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
@WebMvcTest(BusController.class)
@ActiveProfiles("test")
class BusControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private BusService busService;
    @MockBean  private JwtUtils jwtUtils;
    @MockBean  private UserDetailsServiceImpl userDetailsService;
    @MockBean  private TokenRevocationService tokenRevocationService;

    // ── GET /api/admin/buses ────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllBuses_admin_returns200() throws Exception {
        when(busService.getAllBuses()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/admin/buses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].busNumber").value("BUS-001"));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void getAllBuses_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/buses"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllBuses_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/buses"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/admin/buses/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getBus_admin_returns200() throws Exception {
        when(busService.getBus(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/admin/buses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // ── POST /api/admin/buses ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBus_admin_returns200() throws Exception {
        BusRequest req = buildRequest();
        when(busService.createBus(any(BusRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.busNumber").value("BUS-001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBus_invalidRequest_returns400() throws Exception {
        BusRequest req = new BusRequest();
        // missing required fields

        mockMvc.perform(post("/api/admin/buses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/admin/buses/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBus_admin_returns200() throws Exception {
        BusRequest req = buildRequest();
        when(busService.updateBus(eq(1L), any(BusRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(put("/api/admin/buses/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.busNumber").value("BUS-001"));
    }

    // ── DELETE /api/admin/buses/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateBus_admin_returns200() throws Exception {
        doNothing().when(busService).deactivateBus(1L);

        mockMvc.perform(delete("/api/admin/buses/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BusRequest buildRequest() {
        BusRequest req = new BusRequest();
        req.setBusNumber("BUS-001");
        req.setRegistrationNo("REG-001");
        req.setBusType("AC");
        req.setTotalSeats(40);
        return req;
    }

    private BusResponse buildResponse() {
        BusResponse res = new BusResponse();
        res.setId(1L);
        res.setBusNumber("BUS-001");
        res.setRegistrationNo("REG-001");
        res.setBusType("AC");
        res.setTotalSeats(40);
        res.setStatus(BusStatus.ACTIVE);
        return res;
    }
}
