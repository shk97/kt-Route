package com.ktroute.app.reporting;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.ktroute.app.dto.response.CapacityReport;
import com.ktroute.app.dto.response.DashboardStats;
import com.ktroute.app.dto.response.ProfitabilityResponse;
import com.ktroute.app.dto.response.RouteDemandResponse;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
    private static final Font BODY_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private static final BaseColor HEADER_COLOR = new BaseColor(33, 97, 140);

    // ---- CSV Helpers ----

    public ResponseEntity<byte[]> exportRouteDemandCsv(List<RouteDemandResponse> data) throws Exception {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{"Route ID","Route Name","Route Code","Start","End",
                    "Passengers","Trips","Revenue","Avg Occupancy %","Category"});
            for (RouteDemandResponse r : data) {
                writer.writeNext(new String[]{
                        str(r.getRouteId()), r.getRouteName(), safeStr(r.getRouteCode()),
                        safeStr(r.getStartStation()), safeStr(r.getEndStation()),
                        str(r.getPassengerCount()), str(r.getTripCount()),
                        str(r.getRevenue()), str(r.getAverageOccupancy()), r.getUtilizationCategory()
                });
            }
        }
        return csvResponse("route_demand_" + timestamp() + ".csv", sw.toString().getBytes(StandardCharsets.UTF_8));
    }

    public ResponseEntity<byte[]> exportMonthlyDemandCsv(List<DashboardStats.MonthlyDemand> data) throws Exception {
        long maxPax = data.stream().mapToLong(DashboardStats.MonthlyDemand::getPassengerCount).max().orElse(1);
        long minPax = data.stream().mapToLong(DashboardStats.MonthlyDemand::getPassengerCount).min().orElse(0);
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{"Year","Month","Month Name","Bookings","Passengers","Revenue","Demand Level"});
            for (DashboardStats.MonthlyDemand m : data) {
                writer.writeNext(new String[]{
                        str(m.getYear()), str(m.getMonth()), m.getMonthName(),
                        str(m.getBookingCount()), str(m.getPassengerCount()), str(m.getRevenue()),
                        monthDemandLevel(m.getPassengerCount(), maxPax, minPax)
                });
            }
        }
        return csvResponse("monthly_demand_" + timestamp() + ".csv", sw.toString().getBytes(StandardCharsets.UTF_8));
    }

    public ResponseEntity<byte[]> exportCapacityCsv(List<CapacityReport> data) throws Exception {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{"Trip ID","Route","Bus","Departure","Total Seats",
                    "Passengers","Occupancy %","Category","Recommendation"});
            for (CapacityReport c : data) {
                writer.writeNext(new String[]{
                        str(c.getTripId()), c.getRouteName(), c.getBusNumber(),
                        c.getDepartureTime() != null ? c.getDepartureTime().format(FMT) : "",
                        str(c.getTotalSeats()), str(c.getConfirmedPassengers()),
                        str(c.getOccupancyPercentage()), c.getUtilizationCategory(), c.getRecommendation()
                });
            }
        }
        return csvResponse("capacity_" + timestamp() + ".csv", sw.toString().getBytes(StandardCharsets.UTF_8));
    }

    public ResponseEntity<byte[]> exportProfitabilityCsv(List<ProfitabilityResponse> data) throws Exception {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{"Route ID","Route Name","Passengers","Revenue",
                    "Operating Cost","Profit","Profit Margin %","Beneficial","Cost Data Available"});
            for (ProfitabilityResponse p : data) {
                writer.writeNext(new String[]{
                        str(p.getRouteId()), p.getRouteName(), str(p.getPassengerCount()),
                        str(p.getTotalRevenue()), safeStr(p.getTotalOperatingCost()),
                        safeStr(p.getOperatingProfit()), safeStr(p.getProfitMarginPercent()),
                        beneficialFlag(p), str(p.getCostDataAvailable())
                });
            }
        }
        return csvResponse("profitability_" + timestamp() + ".csv", sw.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ---- PDF Helpers ----

    public ResponseEntity<byte[]> exportRouteDemandPdf(List<RouteDemandResponse> data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, bos);
        doc.open();
        addTitle(doc, "Route Demand Report");
        addGeneratedAt(doc);

        PdfPTable table = createTable(new float[]{1,3,2,2,1,2,2,2}, 8);
        addHeaders(table, new String[]{"Route ID","Route Name","Start","End","Trips","Passengers","Revenue","Occupancy %"});
        for (RouteDemandResponse r : data) {
            addCell(table, str(r.getRouteId()));
            addCell(table, r.getRouteName());
            addCell(table, safeStr(r.getStartStation()));
            addCell(table, safeStr(r.getEndStation()));
            addCell(table, str(r.getTripCount()));
            addCell(table, str(r.getPassengerCount()));
            addCell(table, str(r.getRevenue()));
            addCell(table, str(r.getAverageOccupancy()) + "%");
        }
        doc.add(table);
        doc.close();
        return pdfResponse("route_demand_" + timestamp() + ".pdf", bos.toByteArray());
    }

    public ResponseEntity<byte[]> exportMonthlyDemandPdf(List<DashboardStats.MonthlyDemand> data) throws Exception {
        long maxPax = data.stream().mapToLong(DashboardStats.MonthlyDemand::getPassengerCount).max().orElse(1);
        long minPax = data.stream().mapToLong(DashboardStats.MonthlyDemand::getPassengerCount).min().orElse(0);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, bos);
        doc.open();
        addTitle(doc, "Monthly Passenger Demand Report");
        addGeneratedAt(doc);

        PdfPTable table = createTable(new float[]{1, 1.5f, 2.5f, 1.5f, 1.5f, 2.5f, 2}, 7);
        addHeaders(table, new String[]{"Year", "Month", "Month Name", "Bookings", "Passengers", "Revenue", "Demand Level"});
        for (DashboardStats.MonthlyDemand m : data) {
            String level = monthDemandLevel(m.getPassengerCount(), maxPax, minPax);
            addCell(table, str(m.getYear()));
            addCell(table, str(m.getMonth()));
            addCell(table, m.getMonthName());
            addCell(table, str(m.getBookingCount()));
            addCell(table, str(m.getPassengerCount()));
            addCell(table, str(m.getRevenue()));
            addColoredCell(table, level,
                    "HEAVY".equals(level) ? new BaseColor(21, 128, 61) :
                    "LIGHT".equals(level) ? new BaseColor(185, 28, 28) :
                    new BaseColor(37, 99, 235));
        }
        doc.add(table);
        doc.close();
        return pdfResponse("monthly_demand_" + timestamp() + ".pdf", bos.toByteArray());
    }

    public ResponseEntity<byte[]> exportProfitabilityPdf(List<ProfitabilityResponse> data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, bos);
        doc.open();
        addTitle(doc, "Route Profitability Report");
        addGeneratedAt(doc);

        PdfPTable table = createTable(new float[]{0.7f, 2.5f, 1.5f, 1.5f, 1.8f, 1.5f, 1.5f, 1.8f}, 8);
        addHeaders(table, new String[]{"Route ID", "Route Name", "Passengers", "Revenue",
                "Operating Cost", "Profit", "Margin %", "Beneficial"});
        for (ProfitabilityResponse p : data) {
            String flag = beneficialFlag(p);
            addCell(table, str(p.getRouteId()));
            addCell(table, p.getRouteName());
            addCell(table, str(p.getPassengerCount()));
            addCell(table, str(p.getTotalRevenue()));
            addCell(table, safeStr(p.getTotalOperatingCost()));
            addCell(table, safeStr(p.getOperatingProfit()));
            addCell(table, safeStr(p.getProfitMarginPercent()));
            addColoredCell(table, flag,
                    "YES".equals(flag) ? new BaseColor(21, 128, 61) :
                    "NO".equals(flag)  ? new BaseColor(185, 28, 28) :
                    BaseColor.GRAY);
        }
        doc.add(table);
        doc.close();
        return pdfResponse("profitability_" + timestamp() + ".pdf", bos.toByteArray());
    }

    public ResponseEntity<byte[]> exportCapacityPdf(List<CapacityReport> data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, bos);
        doc.open();
        addTitle(doc, "Capacity Utilization Report");
        addGeneratedAt(doc);

        PdfPTable table = createTable(new float[]{1,3,2,2,1,1,2,3}, 8);
        addHeaders(table, new String[]{"Trip ID","Route","Bus","Departure","Seats","Passengers","Occupancy %","Category"});
        for (CapacityReport c : data) {
            addCell(table, str(c.getTripId()));
            addCell(table, c.getRouteName());
            addCell(table, c.getBusNumber());
            addCell(table, c.getDepartureTime() != null ? c.getDepartureTime().format(FMT) : "");
            addCell(table, str(c.getTotalSeats()));
            addCell(table, str(c.getConfirmedPassengers()));
            addCell(table, str(c.getOccupancyPercentage()) + "%");
            addCell(table, c.getUtilizationCategory());
        }
        doc.add(table);
        doc.close();
        return pdfResponse("capacity_" + timestamp() + ".pdf", bos.toByteArray());
    }

    // ---- Utilities ----

    private PdfPTable createTable(float[] widths, int cols) throws DocumentException {
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setSpacingBefore(10f);
        return table;
    }

    private void addHeaders(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(HEADER_COLOR);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", BODY_FONT));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addTitle(Document doc, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, TITLE_FONT);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(10f);
        doc.add(p);
    }

    private void addGeneratedAt(Document doc) throws DocumentException {
        Font small = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);
        Paragraph p = new Paragraph("Generated: " + LocalDateTime.now().format(FMT), small);
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingAfter(10f);
        doc.add(p);
    }

    private ResponseEntity<byte[]> csvResponse(String filename, byte[] data) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    private ResponseEntity<byte[]> pdfResponse(String filename, byte[] data) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
    private String safeStr(Object o) { return o != null ? o.toString() : "N/A"; }

    private String monthDemandLevel(long passengers, long maxPax, long minPax) {
        long range = maxPax - minPax;
        if (range == 0) return "NORMAL";
        double pct = (double)(passengers - minPax) / range;
        if (pct >= 0.7) return "HEAVY";
        if (pct <= 0.3) return "LIGHT";
        return "NORMAL";
    }

    private String beneficialFlag(ProfitabilityResponse p) {
        if (!Boolean.TRUE.equals(p.getCostDataAvailable())) return "N/A";
        if (p.getOperatingProfit() == null) return "N/A";
        return p.getOperatingProfit().compareTo(java.math.BigDecimal.ZERO) > 0 ? "YES" : "NO";
    }

    private void addColoredCell(PdfPTable table, String text, BaseColor color) {
        Font f = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(color);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}
