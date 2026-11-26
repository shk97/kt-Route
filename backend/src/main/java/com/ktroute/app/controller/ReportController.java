package com.ktroute.app.controller;

import com.ktroute.app.dto.response.*;
import com.ktroute.app.reporting.ReportGenerator;
import com.ktroute.app.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final AnalyticsService analyticsService;
    private final ReportGenerator reportGenerator;

    @GetMapping("/routes")
    public ResponseEntity<byte[]> routesReport(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) throws Exception {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        List<RouteDemandResponse> data = analyticsService.getRouteDemand(f, t, "passengers_desc");
        if ("pdf".equalsIgnoreCase(format)) return reportGenerator.exportRouteDemandPdf(data);
        return reportGenerator.exportRouteDemandCsv(data);
    }

    @GetMapping("/monthly")
    public ResponseEntity<byte[]> monthlyReport(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "2024") int fromYear,
            @RequestParam(defaultValue = "2026") int toYear) throws Exception {
        List<DashboardStats.MonthlyDemand> data = analyticsService.getMonthlyDemand(fromYear, toYear);
        if ("pdf".equalsIgnoreCase(format)) return reportGenerator.exportMonthlyDemandPdf(data);
        return reportGenerator.exportMonthlyDemandCsv(data);
    }

    @GetMapping("/capacity")
    public ResponseEntity<byte[]> capacityReport(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) throws Exception {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusMonths(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now().plusMonths(1);
        List<CapacityReport> data = analyticsService.getCapacityReport(f, t);
        if ("pdf".equalsIgnoreCase(format)) return reportGenerator.exportCapacityPdf(data);
        return reportGenerator.exportCapacityCsv(data);
    }

    @GetMapping("/profitability")
    public ResponseEntity<byte[]> profitabilityReport(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) throws Exception {
        LocalDateTime f = from != null ? from : LocalDateTime.now().minusYears(1);
        LocalDateTime t = to != null ? to : LocalDateTime.now();
        List<ProfitabilityResponse> data = analyticsService.getProfitability(f, t);
        if ("pdf".equalsIgnoreCase(format)) return reportGenerator.exportProfitabilityPdf(data);
        return reportGenerator.exportProfitabilityCsv(data);
    }
}
