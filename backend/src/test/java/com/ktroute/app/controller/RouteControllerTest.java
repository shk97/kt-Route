package com.ktroute.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktroute.app.dto.request.RouteRequest;
import com.ktroute.app.dto.response.RouteResponse;
import com.ktroute.app.enums.RouteStatus;
import com.ktroute.app.security.UserDetailsServiceImpl;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.RouteService;
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
@WebMvcTest(RouteController.class)
@ActiveProfiles("test")
class RouteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private RouteService routeService;
    @MockBean  private JwtUtils jwtUtils;
    @MockBean  private UserDetailsServiceImpl userDetailsService;
    @MockBean  private TokenRevocationService tokenRevocationService;

    // ── Public: GET /api/routes ───────────────────────────────────────────────

    @Test
    @WithMockUser
    void getAllRoutes_authenticated_returns200() throws Exception {
        when(routeService.getActiveRoutes()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].routeCode").value("AB-EX"));
    }

    // ── Public: GET /api/routes/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser
    void getRoute_returns200() throws Exception {
        when(routeService.getRoute(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/routes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("A-B Express"));
    }

    // ── Admin: GET /api/admin/routes ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllRoutesAdmin_admin_returns200() throws Exception {
        when(routeService.getAllRoutes()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/admin/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void getAllRoutesAdmin_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/routes"))
                .andExpect(status().isForbidden());
    }

    // ── Admin: POST /api/admin/routes ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRoute_admin_returns200() throws Exception {
        RouteRequest req = buildRequest();
        when(routeService.createRoute(any(RouteRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/admin/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeCode").value("AB-EX"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRoute_missingRequired_returns400() throws Exception {
        RouteRequest req = new RouteRequest();

        mockMvc.perform(post("/api/admin/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void createRoute_nonAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    // ── Admin: PUT /api/admin/routes/{id} ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoute_admin_returns200() throws Exception {
        when(routeService.updateRoute(eq(1L), any(RouteRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(put("/api/admin/routes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());
    }

    // ── Admin: DELETE /api/admin/routes/{id} ──────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateRoute_admin_returns200() throws Exception {
        doNothing().when(routeService).deactivateRoute(1L);

        mockMvc.perform(delete("/api/admin/routes/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RouteRequest buildRequest() {
        RouteRequest req = new RouteRequest();
        req.setName("A-B Express");
        req.setRouteCode("AB-EX");
        req.setStartStationId(1L);
        req.setEndStationId(2L);
        req.setDistanceKm(120.0);
        req.setEstimatedDurationMinutes(180);
        return req;
    }

    private RouteResponse buildResponse() {
        RouteResponse res = new RouteResponse();
        res.setId(1L);
        res.setName("A-B Express");
        res.setRouteCode("AB-EX");
        res.setDistanceKm(120.0);
        res.setEstimatedDurationMinutes(180);
        res.setStatus(RouteStatus.ACTIVE);
        return res;
    }
}
