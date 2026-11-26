package com.ktroute.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktroute.app.dto.request.StationRequest;
import com.ktroute.app.dto.response.StationResponse;
import com.ktroute.app.security.UserDetailsServiceImpl;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.StationService;
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
@WebMvcTest(StationController.class)
@ActiveProfiles("test")
class StationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private StationService stationService;
    @MockBean  private JwtUtils jwtUtils;
    @MockBean  private UserDetailsServiceImpl userDetailsService;
    @MockBean  private TokenRevocationService tokenRevocationService;

    // ── Public: GET /api/stations ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void getAllStations_authenticated_returns200() throws Exception {
        when(stationService.getActiveStations()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("CST"));
    }

    // ── Public: GET /api/stations/{id} ────────────────────────────────────────

    @Test
    @WithMockUser
    void getStation_returns200() throws Exception {
        when(stationService.getStation(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Central Station"));
    }

    // ── Admin: POST /api/admin/stations ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStation_admin_returns200() throws Exception {
        StationRequest req = buildRequest();
        when(stationService.createStation(any(StationRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/admin/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("CST"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStation_missingRequired_returns400() throws Exception {
        StationRequest req = new StationRequest();

        mockMvc.perform(post("/api/admin/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void createStation_nonAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    // ── Admin: PUT /api/admin/stations/{id} ───────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStation_admin_returns200() throws Exception {
        when(stationService.updateStation(eq(1L), any(StationRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(put("/api/admin/stations/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());
    }

    // ── Admin: DELETE /api/admin/stations/{id} ────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateStation_admin_returns200() throws Exception {
        doNothing().when(stationService).deactivateStation(1L);

        mockMvc.perform(delete("/api/admin/stations/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void deactivateStation_nonAdmin_returns403() throws Exception {
        mockMvc.perform(delete("/api/admin/stations/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StationRequest buildRequest() {
        StationRequest req = new StationRequest();
        req.setName("Central Station");
        req.setCode("CST");
        req.setLocation("Main Road");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        return req;
    }

    private StationResponse buildResponse() {
        StationResponse res = new StationResponse();
        res.setId(1L);
        res.setName("Central Station");
        res.setCode("CST");
        res.setLocation("Main Road");
        res.setCity("Mumbai");
        res.setState("Maharashtra");
        res.setIsActive(true);
        return res;
    }
}
