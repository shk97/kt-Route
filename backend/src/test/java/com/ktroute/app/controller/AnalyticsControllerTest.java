package com.ktroute.app.controller;

import com.ktroute.app.dto.response.*;
import com.ktroute.app.security.UserDetailsServiceImpl;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.AnalyticsService;
import com.ktroute.app.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(AnalyticsController.class)
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private AnalyticsService analyticsService;
    @MockBean  private JwtUtils jwtUtils;
    @MockBean  private UserDetailsServiceImpl userDetailsService;
    @MockBean  private TokenRevocationService tokenRevocationService;

    // ── GET /api/admin/analytics/dashboard ────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDashboard_admin_returns200() throws Exception {
        DashboardStats stats = DashboardStats.builder()
                .totalActiveRoutes(5).totalBuses(10).totalPassengers(100L).build();
        when(analyticsService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalActiveRoutes").value(5));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void getDashboard_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboard_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/admin/analytics/routes ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRouteDemand_admin_returns200() throws Exception {
        RouteDemandResponse r = RouteDemandResponse.builder()
                .routeId(1L).routeName("A-B").passengerCount(50L).build();
        when(analyticsService.getRouteDemand(any(), any(), anyString())).thenReturn(List.of(r));

        mockMvc.perform(get("/api/admin/analytics/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].routeName").value("A-B"));
    }

    // ── GET /api/admin/analytics/capacity ────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCapacity_admin_returns200() throws Exception {
        when(analyticsService.getCapacityReport(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/capacity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── GET /api/admin/analytics/monthly ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMonthly_admin_returns200() throws Exception {
        when(analyticsService.getMonthlyDemand(anyInt(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── GET /api/admin/analytics/profitability ────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProfitability_admin_returns200() throws Exception {
        when(analyticsService.getProfitability(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/profitability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── GET /api/admin/analytics/stations/overview ────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStationOverview_admin_returns200() throws Exception {
        StationAnalyticsResponse.Overview overview = StationAnalyticsResponse.Overview.builder()
                .totalActiveStations(10).totalBoardingPassengers(500L).build();
        when(analyticsService.getStationOverview(any(), any())).thenReturn(overview);

        mockMvc.perform(get("/api/admin/analytics/stations/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveStations").value(10));
    }

    // ── GET /api/admin/analytics/stations/ranked ──────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStationRanked_admin_returns200() throws Exception {
        when(analyticsService.getStationBoardingRanked(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/stations/ranked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── GET /api/admin/analytics/stations/map ────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStationMap_admin_returns200() throws Exception {
        when(analyticsService.getMapStations(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/stations/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── GET /api/admin/analytics/stations/recommendations ────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRecommendations_admin_returns200() throws Exception {
        when(analyticsService.getRecommendations(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/analytics/stations/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
