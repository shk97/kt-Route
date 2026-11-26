package com.ktroute.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktroute.app.dto.request.TripRequest;
import com.ktroute.app.dto.response.TripResponse;
import com.ktroute.app.enums.TripStatus;
import com.ktroute.app.security.UserDetailsServiceImpl;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.TripService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(TripController.class)
@ActiveProfiles("test")
class TripControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private TripService tripService;
    @MockBean  private JwtUtils jwtUtils;
    @MockBean  private UserDetailsServiceImpl userDetailsService;
    @MockBean  private TokenRevocationService tokenRevocationService;

    // ── Public: GET /api/trips/search ─────────────────────────────────────────

    @Test
    @WithMockUser
    void searchTrips_returns200() throws Exception {
        when(tripService.searchTrips(eq(1L), eq(2L), any())).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/trips/search")
                        .param("startStationId", "1")
                        .param("endStationId", "2")
                        .param("date", "2026-12-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SCHEDULED"));
    }

    // ── Public: GET /api/trips/{id} ───────────────────────────────────────────

    @Test
    @WithMockUser
    void getTrip_returns200() throws Exception {
        when(tripService.getTrip(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/trips/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // ── Admin: GET /api/admin/trips ───────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllTrips_admin_returns200() throws Exception {
        when(tripService.getAllTrips()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/admin/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void getAllTrips_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/trips"))
                .andExpect(status().isForbidden());
    }

    // ── Admin: GET /api/admin/trips/route/{routeId} ───────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTripsByRoute_admin_returns200() throws Exception {
        when(tripService.getTripsByRoute(1L)).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/admin/trips/route/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── Admin: POST /api/admin/trips ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void createTrip_admin_returns200() throws Exception {
        TripRequest req = buildRequest();
        when(tripService.createTrip(any(TripRequest.class), eq("admin@test.com"))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/admin/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void createTrip_nonAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/trips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    // ── Admin: PUT /api/admin/trips/{id} ──────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void updateTrip_admin_returns200() throws Exception {
        when(tripService.updateTrip(eq(1L), any(TripRequest.class), eq("admin@test.com"))).thenReturn(buildResponse());

        mockMvc.perform(put("/api/admin/trips/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());
    }

    // ── Admin: POST /api/admin/trips/{id}/cancel ──────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void cancelTrip_admin_returns200() throws Exception {
        doNothing().when(tripService).cancelTrip(eq(1L), anyString(), eq("admin@test.com"));

        mockMvc.perform(post("/api/admin/trips/1/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Maintenance\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TripRequest buildRequest() {
        TripRequest req = new TripRequest();
        req.setRouteId(1L);
        req.setBusId(1L);
        req.setDepartureTime(LocalDateTime.now().plusDays(1));
        req.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(3));
        req.setBasePrice(new BigDecimal("200.00"));
        return req;
    }

    private TripResponse buildResponse() {
        TripResponse res = new TripResponse();
        res.setId(1L);
        res.setStatus(TripStatus.SCHEDULED);
        res.setBasePrice(new BigDecimal("200.00"));
        res.setTotalSeats(40);
        res.setAvailableSeats(40);
        res.setConfirmedPassengers(0);
        return res;
    }
}
