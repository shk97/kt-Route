package com.ktroute.app.service;

import com.ktroute.app.dto.request.BookingRequest;
import com.ktroute.app.dto.response.BookingResponse;
import java.util.List;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request, String userEmail);
    BookingResponse getBooking(Long id, String userEmail);
    BookingResponse getBookingByRef(String ref, String userEmail);
    List<BookingResponse> getMyBookings(String userEmail);
    List<BookingResponse> getAllBookings();
    BookingResponse cancelBooking(Long id, String reason, String userEmail);
    List<BookingResponse> getBookingsByTrip(Long tripId);
}
