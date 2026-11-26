package com.ktroute.app.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.ktroute.app.entity.Booking;
import com.ktroute.app.entity.BookingPassenger;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class TicketGeneratorService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Font BRAND_FONT   = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,  new BaseColor(33, 97, 140));
    private static final Font TITLE_FONT   = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,  BaseColor.DARK_GRAY);
    private static final Font LABEL_FONT   = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,  new BaseColor(80, 80, 80));
    private static final Font VALUE_FONT   = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.BLACK);
    private static final Font HEADER_FONT  = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,  BaseColor.WHITE);
    private static final Font SMALL_FONT   = new Font(Font.FontFamily.HELVETICA, 7,  Font.ITALIC, BaseColor.GRAY);
    private static final BaseColor BRAND_COLOR = new BaseColor(33, 97, 140);
    private static final BaseColor ALT_ROW     = new BaseColor(240, 248, 255);

    public byte[] generateTicket(Booking booking) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, bos);
            doc.open();

            addHeader(doc, booking);
            addDivider(doc);
            addTripDetails(doc, booking);
            addDivider(doc);
            addPassengerTable(doc, booking);
            addDivider(doc);
            addFooter(doc, booking);

            doc.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate e-ticket PDF", e);
        }
    }

    private void addHeader(Document doc, Booking booking) throws DocumentException {
        Paragraph brand = new Paragraph("RouteSync", BRAND_FONT);
        brand.setAlignment(Element.ALIGN_CENTER);
        doc.add(brand);

        Paragraph subtitle = new Paragraph("Electronic Bus Ticket", TITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingBefore(4);
        doc.add(subtitle);

        Paragraph ref = new Paragraph("Booking Ref: " + booking.getBookingRef(), LABEL_FONT);
        ref.setAlignment(Element.ALIGN_CENTER);
        ref.setSpacingBefore(6);
        doc.add(ref);
    }

    private void addDivider(Document doc) throws DocumentException {
        LineSeparator line = new LineSeparator(1f, 100f, BRAND_COLOR, Element.ALIGN_CENTER, -4);
        doc.add(new Chunk(line));
        doc.add(Chunk.NEWLINE);
    }

    private void addTripDetails(Document doc, Booking booking) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1});
        table.setSpacingBefore(8);
        table.setSpacingAfter(8);

        String route    = booking.getTrip().getRoute().getName();
        String boarding = booking.getBoardingStation() != null ? booking.getBoardingStation().getName() : "—";
        String alighting= booking.getAlightingStation() != null ? booking.getAlightingStation().getName() : "—";
        String departure = booking.getTrip().getDepartureTime() != null
                ? booking.getTrip().getDepartureTime().format(DT_FMT) : "—";
        String arrival  = booking.getTrip().getArrivalTime() != null
                ? booking.getTrip().getArrivalTime().format(DT_FMT) : "—";
        String bus      = booking.getTrip().getBus().getBusNumber();

        addInfoRow(table, "Route",          route);
        addInfoRow(table, "Departure",      departure);
        addInfoRow(table, "Boarding From",  boarding);
        addInfoRow(table, "Arrival",        arrival);
        addInfoRow(table, "Alighting At",   alighting);
        addInfoRow(table, "Bus Number",     bus);
        addInfoRow(table, "Passengers",     String.valueOf(booking.getPassengerCount()));
        addInfoRow(table, "Total Fare",     "₹ " + booking.getTotalAmount());

        doc.add(table);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        labelCell.setBackgroundColor(BaseColor.WHITE);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "—", VALUE_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addPassengerTable(Document doc, Booking booking) throws DocumentException {
        Paragraph title = new Paragraph("Passenger Details", TITLE_FONT);
        title.setSpacingBefore(6);
        title.setSpacingAfter(6);
        doc.add(title);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 2.5f, 1f, 1.5f});

        for (String h : new String[]{"#", "Passenger Name", "Age", "Seat No."}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
            cell.setBackgroundColor(BRAND_COLOR);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        int i = 1;
        for (BookingPassenger p : booking.getPassengers()) {
            BaseColor bg = (i % 2 == 0) ? ALT_ROW : BaseColor.WHITE;
            addTableCell(table, String.valueOf(i++), bg, Element.ALIGN_CENTER);
            addTableCell(table, p.getPassengerName(), bg, Element.ALIGN_LEFT);
            addTableCell(table, String.valueOf(p.getAge()), bg, Element.ALIGN_CENTER);
            addTableCell(table, p.getSeatNumber(), bg, Element.ALIGN_CENTER);
        }

        doc.add(table);
    }

    private void addTableCell(PdfPTable table, String text, BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, VALUE_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addFooter(Document doc, Booking booking) throws DocumentException {
        String bookedOn = booking.getCreatedAt() != null
                ? booking.getCreatedAt().format(DT_FMT) : "—";
        Paragraph footer = new Paragraph(
                "Booked on: " + bookedOn + "   |   Please carry a valid photo ID while travelling.",
                SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        doc.add(footer);

        Paragraph terms = new Paragraph(
                "This is a system-generated ticket and does not require a signature. "
                + "Cancellation is subject to our cancellation policy.",
                SMALL_FONT);
        terms.setAlignment(Element.ALIGN_CENTER);
        terms.setSpacingBefore(4);
        doc.add(terms);
    }
}
