package com.ktroute.app.dto.response;

import com.ktroute.app.enums.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Long id;
    private String bookingRef;
    private TripResponse trip;
    private Integer passengerCount;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private RouteResponse.StationSummary boardingStation;
    private RouteResponse.StationSummary alightingStation;
    private List<PassengerDetail> passengers;
    private LocalDateTime createdAt;

    // Populated after payment is initiated — lets the frontend redirect to payment confirmation
    private String paymentRef;

    @Data
    public static class PassengerDetail {
        private Long id;
        private String passengerName;
        private Integer age;
        private String seatNumber;
        private BigDecimal ticketPrice;
    }
}

