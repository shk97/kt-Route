package com.ktroute.app.service.impl;

import com.ktroute.app.dto.request.*;
import com.ktroute.app.dto.response.*;
import com.ktroute.app.entity.User;
import com.ktroute.app.enums.RoleName;
import com.ktroute.app.exception.AppException;
import com.ktroute.app.exception.DuplicateResourceException;
import com.ktroute.app.repository.RoleRepository;
import com.ktroute.app.repository.UserRepository;
import com.ktroute.app.security.jwt.JwtUtils;
import com.ktroute.app.security.service.LoginAttemptService;
import com.ktroute.app.security.service.TokenRevocationService;
import com.ktroute.app.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository       userRepository;
    private final RoleRepository       roleRepository;
    private final PasswordEncoder      passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtils             jwtUtils;
    private final LoginAttemptService  loginAttemptService;
    private final TokenRevocationService revocationService;

    @Value("${app.admin.registration.code:}")
    private String adminRegistrationCode;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }

        boolean wantsAdmin = "ADMIN".equalsIgnoreCase(req.getRole());
        if (wantsAdmin) {
            if (adminRegistrationCode.isBlank() || !adminRegistrationCode.equals(req.getAdminCode())) {
                throw new AppException("Invalid admin registration code", HttpStatus.FORBIDDEN);
            }
        }

        RoleName roleName = wantsAdmin ? RoleName.ROLE_ADMIN : RoleName.ROLE_PASSENGER;
        var assignedRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AppException("Roles not initialized", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = User.builder()
                .fullName(req.getFullName().trim())
                .email(email)
                .phone(req.getPhone())
                .password(passwordEncoder.encode(req.getPassword()))
                .roles(Set.of(assignedRole))
                .isActive(true)
                .build();

        userRepository.save(user);
        String token = jwtUtils.generateToken(user.getEmail());
        return buildAuthResponse(user, token);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail().toLowerCase().trim();

        if (loginAttemptService.isLocked(email)) {
            throw new AppException(
                    "Account temporarily locked due to repeated failures. Try again in 15 minutes.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.getPassword()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(email);
            int remaining = loginAttemptService.remainingAttempts(email);
            String msg = remaining > 0
                    ? "Invalid credentials. " + remaining + " attempt(s) remaining."
                    : "Invalid credentials. Account is now locked for 15 minutes.";
            throw new AppException(msg, HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException("Account is disabled. Please contact support.", HttpStatus.FORBIDDEN);
        }

        loginAttemptService.reset(email);
        return buildAuthResponse(user, jwtUtils.generateToken(email));
    }

    @Override
    public void logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtils.validateToken(token)) {
                revocationService.revoke(token, jwtUtils.getExpiry(token));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return mapToUserResponse(user);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setFullName(user.getFullName());
        res.setEmail(user.getEmail());
        res.setPhone(user.getPhone());
        res.setIsActive(user.getIsActive());
        res.setRoles(user.getRoles().stream()
                .map(r -> r.getName().name()).collect(Collectors.toSet()));
        res.setCreatedAt(user.getCreatedAt());
        return res;
    }
}
