package com.ktroute.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktroute.app.dto.request.PaymentConfirmRequest;
import com.ktroute.app.dto.request.PaymentInitiateRequest;
import com.ktroute.app.dto.response.PaymentResponse;
import com.ktroute.app.enums.PaymentMethod;
import com.ktroute.app.enums.PaymentStatus;
import com.ktroute.app.security.UserDetailsServiceImpl;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.PaymentService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private TokenRevocationService tokenRevocationService;

    @Test
    @WithMockUser(username = "passenger@test.com", roles = {"PASSENGER"})
    void initiatePayment_returns200() throws Exception {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setBookingId(1L);
        req.setMethod(PaymentMethod.UPI);

        when(paymentService.initiatePayment(any(PaymentInitiateRequest.class), eq("passenger@test.com")))
                .thenReturn(buildResponse(PaymentStatus.PROCESSING));

        mockMvc.perform(post("/api/payments/initiate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.paymentRef").value("PAY001"));
    }

    @Test
    @WithMockUser(username = "passenger@test.com", roles = {"PASSENGER"})
    void confirmPayment_returns200() throws Exception {
        when(paymentService.confirmPayment(eq("PAY001"), any(PaymentConfirmRequest.class), eq("passenger@test.com")))
                .thenReturn(buildResponse(PaymentStatus.COMPLETED));

        PaymentConfirmRequest confirmReq = new PaymentConfirmRequest();
        confirmReq.setRazorpayPaymentId("pay_test123");
        confirmReq.setRazorpayOrderId("order_test123");
        confirmReq.setRazorpaySignature("sig_test123");

        mockMvc.perform(post("/api/payments/PAY001/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "passenger@test.com", roles = {"PASSENGER"})
    void failPayment_returns200() throws Exception {
        when(paymentService.failPayment(eq("PAY001"), anyString(), eq("passenger@test.com")))
                .thenReturn(buildResponse(PaymentStatus.FAILED));

        mockMvc.perform(post("/api/payments/PAY001/fail")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Card declined\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    @Test
    @WithMockUser(username = "passenger@test.com", roles = {"PASSENGER"})
    void getPayment_returns200() throws Exception {
        when(paymentService.getPayment(eq("PAY001"), eq("passenger@test.com")))
                .thenReturn(buildResponse(PaymentStatus.COMPLETED));

        mockMvc.perform(get("/api/payments/PAY001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentRef").value("PAY001"));
    }

    @Test
    void initiatePayment_unauthenticated_returns403() throws Exception {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setBookingId(1L);
        req.setMethod(PaymentMethod.UPI);

        mockMvc.perform(post("/api/payments/initiate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    private PaymentResponse buildResponse(PaymentStatus status) {
        PaymentResponse r = new PaymentResponse();
        r.setId(1L);
        r.setPaymentRef("PAY001");
        r.setBookingId(1L);
        r.setBookingRef("BK001");
        r.setAmount(new BigDecimal("500.00"));
        r.setStatus(status);
        r.setMethod(PaymentMethod.UPI);
        return r;
    }
}
