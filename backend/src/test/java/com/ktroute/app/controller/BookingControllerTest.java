package com.ktroute.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktroute.app.dto.request.BookingRequest;
import com.ktroute.app.dto.response.BookingResponse;
import com.ktroute.app.enums.BookingStatus;
import com.ktroute.app.security.UserDetailsServiceImpl;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.BookingService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(BookingController.class)
@ActiveProfiles("test")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private TokenRevocationService tokenRevocationService;

    // ── POST /api/bookings ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "passenger@test.com", roles = {"PASSENGER"})
    void createBooking_returns200() throws Exception {
        BookingRequest req = buildRequest();
        BookingResponse resp = buildResponse(BookingStatus.PENDING);

        when(bookingService.createBooking(any(BookingRequest.class), eq("passenger@test.com")))
                .thenReturn(resp);

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingRef").value("BK001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createBooking_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/bookings/my ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "passenger@test.com", roles = {"PASSENGER"})
    void getMyBookings_returns200() throws Exception {
        when(bookingService.getMyBookings("passenger@test.com"))
                .thenReturn(List.of(buildResponse(BookingStatus.CONFIRMED)));

        mockMvc.perform(get("/api/bookings/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"));
    }

    // ── GET /api/bookings/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "passenger@test.com", roles = {"PASSENGER"})
    void getBooking_returns200() throws Exception {
        when(bookingService.getBooking(1L, "passenger@test.com"))
                .thenReturn(buildResponse(BookingStatus.CONFIRMED));

        mockMvc.perform(get("/api/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingRef").value("BK001"));
    }

    // ── POST /api/bookings/{id}/cancel ────────────────────────────────────────

    @Test
    @WithMockUser(username = "passenger@test.com", roles = {"PASSENGER"})
    void cancelBooking_returns200() throws Exception {
        BookingResponse resp = buildResponse(BookingStatus.CANCELLED);
        when(bookingService.cancelBooking(eq(1L), anyString(), eq("passenger@test.com")))
                .thenReturn(resp);

        mockMvc.perform(post("/api/bookings/1/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Changed plans\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ── Admin: GET /api/bookings/admin/all ────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void getAllBookings_admin_returns200() throws Exception {
        when(bookingService.getAllBookings())
                .thenReturn(List.of(buildResponse(BookingStatus.CONFIRMED)));

        mockMvc.perform(get("/api/bookings/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "passenger@test.com", roles = {"PASSENGER"})
    void getAllBookings_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/bookings/admin/all"))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BookingRequest buildRequest() {
        BookingRequest req = new BookingRequest();
        req.setTripId(1L);
        req.setBoardingStationId(1L);
        req.setAlightingStationId(2L);
        BookingRequest.PassengerDetail pd = new BookingRequest.PassengerDetail();
        pd.setPassengerName("Alice");
        pd.setAge(25);
        pd.setSeatNumber("A1");
        req.setPassengers(List.of(pd));
        return req;
    }

    private BookingResponse buildResponse(BookingStatus status) {
        BookingResponse resp = new BookingResponse();
        resp.setId(1L);
        resp.setBookingRef("BK001");
        resp.setStatus(status);
        resp.setPassengerCount(1);
        resp.setTotalAmount(new BigDecimal("250.00"));
        resp.setPassengers(List.of());
        return resp;
    }
}
