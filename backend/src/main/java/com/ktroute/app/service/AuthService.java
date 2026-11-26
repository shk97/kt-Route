package com.ktroute.app.service;

import com.ktroute.app.dto.request.LoginRequest;
import com.ktroute.app.dto.request.RegisterRequest;
import com.ktroute.app.dto.response.AuthResponse;
import com.ktroute.app.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getCurrentUser(String email);
    void logout(HttpServletRequest request);
}
