package com.ktroute.app.controller;

import com.ktroute.app.reporting.ReportGenerator;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(ReportController.class)
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private AnalyticsService analyticsService;
    @MockBean  private ReportGenerator reportGenerator;
    @MockBean  private JwtUtils jwtUtils;
    @MockBean  private UserDetailsServiceImpl userDetailsService;
    @MockBean  private TokenRevocationService tokenRevocationService;

    private static final byte[] CSV_BYTES  = "id,name\n1,A-B\n".getBytes();
    private static final byte[] PDF_BYTES  = "%PDF-1.4 fake".getBytes();

    // ── GET /api/admin/reports/routes ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void routesReport_csv_admin_returns200() throws Exception {
        when(analyticsService.getRouteDemand(any(), any(), anyString())).thenReturn(List.of());
        when(reportGenerator.exportRouteDemandCsv(anyList()))
                .thenReturn(ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=routes.csv")
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .body(CSV_BYTES));

        mockMvc.perform(get("/api/admin/reports/routes").param("format", "csv"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void routesReport_pdf_admin_returns200() throws Exception {
        when(analyticsService.getRouteDemand(any(), any(), anyString())).thenReturn(List.of());
        when(reportGenerator.exportRouteDemandPdf(anyList()))
                .thenReturn(ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=routes.pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(PDF_BYTES));

        mockMvc.perform(get("/api/admin/reports/routes").param("format", "pdf"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void routesReport_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/reports/routes"))
                .andExpect(status().isForbidden());
    }

    @Test
    void routesReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/reports/routes"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/admin/reports/monthly ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void monthlyReport_csv_returns200() throws Exception {
        when(analyticsService.getMonthlyDemand(anyInt(), anyInt())).thenReturn(List.of());
        when(reportGenerator.exportMonthlyDemandCsv(anyList()))
                .thenReturn(ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .body(CSV_BYTES));

        mockMvc.perform(get("/api/admin/reports/monthly").param("format", "csv"))
                .andExpect(status().isOk());
    }

    // ── GET /api/admin/reports/capacity ───────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void capacityReport_csv_returns200() throws Exception {
        when(analyticsService.getCapacityReport(any(), any())).thenReturn(List.of());
        when(reportGenerator.exportCapacityCsv(anyList()))
                .thenReturn(ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .body(CSV_BYTES));

        mockMvc.perform(get("/api/admin/reports/capacity").param("format", "csv"))
                .andExpect(status().isOk());
    }

    // ── GET /api/admin/reports/profitability ──────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void profitabilityReport_csv_returns200() throws Exception {
        when(analyticsService.getProfitability(any(), any())).thenReturn(List.of());
        when(reportGenerator.exportProfitabilityCsv(anyList()))
                .thenReturn(ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .body(CSV_BYTES));

        mockMvc.perform(get("/api/admin/reports/profitability").param("format", "csv"))
                .andExpect(status().isOk());
    }
}
