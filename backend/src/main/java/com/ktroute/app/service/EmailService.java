package com.ktroute.app.service;

import com.ktroute.app.entity.Booking;
import com.ktroute.app.repository.BookingRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final JavaMailSender mailSender;
    private final TicketGeneratorService ticketGeneratorService;
    private final BookingRepository bookingRepository;

    @Value("${spring.mail.username:noreply@routesync.com}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Async
    @Transactional(readOnly = true)
    public void sendBookingConfirmationWithTicket(Booking booking) {
        if (!mailEnabled) {
            log.info("Mail disabled — skipping confirmation email for booking {}", booking.getBookingRef());
            return;
        }
        try {
            // Reload with all lazy associations — the @Async thread has no open session
            Booking b = bookingRepository.findByIdWithAllAssociations(booking.getId())
                    .orElse(booking);

            byte[] ticketPdf = ticketGeneratorService.generateTicket(b);
            String toEmail   = b.getUser().getEmail();
            String subject   = "Your RouteSync Ticket — " + b.getBookingRef();
            String body      = buildHtmlBody(b);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.addAttachment(
                    "RouteSync_Ticket_" + b.getBookingRef() + ".pdf",
                    new org.springframework.core.io.ByteArrayResource(ticketPdf),
                    "application/pdf"
            );

            mailSender.send(message);
            log.info("Booking confirmation email sent to {} for booking {}", toEmail, b.getBookingRef());
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email for booking {}: {}", booking.getBookingRef(), e.getMessage());
        }
    }

    @Async
    public void sendCancellationEmail(Booking booking) {
        if (!mailEnabled) return;
        try {
            String toEmail = booking.getUser().getEmail();
            String subject = "Booking Cancelled — " + booking.getBookingRef();
            String body    = buildCancellationHtml(booking);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("Cancellation email sent to {} for booking {}", toEmail, booking.getBookingRef());
        } catch (MessagingException e) {
            log.error("Failed to send cancellation email for booking {}: {}", booking.getBookingRef(), e.getMessage());
        }
    }

    private String buildHtmlBody(Booking booking) {
        String departure = booking.getTrip().getDepartureTime() != null
                ? booking.getTrip().getDepartureTime().format(DT_FMT) : "—";
        String boarding  = booking.getBoardingStation() != null ? booking.getBoardingStation().getName() : "—";
        String alighting = booking.getAlightingStation() != null ? booking.getAlightingStation().getName() : "—";

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"/></head>
            <body style="font-family:Arial,sans-serif;background:#f4f6f8;margin:0;padding:0;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:30px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                    <tr><td style="background:#21618c;padding:24px 32px;">
                      <h1 style="color:#ffffff;margin:0;font-size:22px;">RouteSync</h1>
                      <p style="color:#cce0f0;margin:4px 0 0;">Your booking is confirmed!</p>
                    </td></tr>
                    <tr><td style="padding:28px 32px;">
                      <p style="color:#333;font-size:15px;">Hi %s,</p>
                      <p style="color:#555;">Your booking has been confirmed. Please find your e-ticket attached to this email.</p>
                      <table width="100%%" style="border-collapse:collapse;margin-top:16px;">
                        <tr style="background:#f0f8ff;">
                          <td style="padding:10px 12px;color:#21618c;font-weight:bold;width:40%%;">Booking Ref</td>
                          <td style="padding:10px 12px;font-weight:bold;font-size:16px;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 12px;color:#555;">Route</td>
                          <td style="padding:10px 12px;">%s</td>
                        </tr>
                        <tr style="background:#f0f8ff;">
                          <td style="padding:10px 12px;color:#555;">Departure</td>
                          <td style="padding:10px 12px;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 12px;color:#555;">Boarding From</td>
                          <td style="padding:10px 12px;">%s</td>
                        </tr>
                        <tr style="background:#f0f8ff;">
                          <td style="padding:10px 12px;color:#555;">Alighting At</td>
                          <td style="padding:10px 12px;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 12px;color:#555;">Passengers</td>
                          <td style="padding:10px 12px;">%d</td>
                        </tr>
                        <tr style="background:#f0f8ff;">
                          <td style="padding:10px 12px;color:#555;">Total Fare</td>
                          <td style="padding:10px 12px;font-weight:bold;">₹ %s</td>
                        </tr>
                      </table>
                      <p style="color:#777;font-size:12px;margin-top:24px;">Please carry a valid photo ID while travelling. This ticket is non-transferable.</p>
                    </td></tr>
                    <tr><td style="background:#f4f6f8;padding:16px 32px;text-align:center;">
                      <p style="color:#aaa;font-size:11px;margin:0;">© 2026 RouteSync. All rights reserved.</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(
                booking.getUser().getFullName(),
                booking.getBookingRef(),
                booking.getTrip().getRoute().getName(),
                departure,
                boarding,
                alighting,
                booking.getPassengerCount(),
                booking.getTotalAmount()
        );
    }

    private String buildCancellationHtml(Booking booking) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"/></head>
            <body style="font-family:Arial,sans-serif;background:#f4f6f8;margin:0;padding:0;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;padding:30px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;">
                    <tr><td style="background:#c0392b;padding:24px 32px;">
                      <h1 style="color:#ffffff;margin:0;font-size:22px;">RouteSync</h1>
                      <p style="color:#fcc;margin:4px 0 0;">Booking Cancelled</p>
                    </td></tr>
                    <tr><td style="padding:28px 32px;">
                      <p style="color:#333;">Hi %s,</p>
                      <p style="color:#555;">Your booking <strong>%s</strong> has been cancelled.</p>
                      %s
                      <p style="color:#777;font-size:12px;margin-top:20px;">If you have any questions, please contact our support team.</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(
                booking.getUser().getFullName(),
                booking.getBookingRef(),
                booking.getCancellationReason() != null
                        ? "<p style=\"color:#555;\">Reason: " + booking.getCancellationReason() + "</p>"
                        : ""
        );
    }
}
