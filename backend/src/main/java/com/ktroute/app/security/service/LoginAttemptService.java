package com.ktroute.app.security.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login attempt tracker. Locks an email for LOCKOUT_MINUTES after
 * MAX_ATTEMPTS consecutive failures. State resets on server restart — acceptable
 * for this environment; replace with a persistent store (Redis / DB) for production.
 */
@Service
public class LoginAttemptService {

    private static final int    MAX_ATTEMPTS      = 5;
    private static final long   LOCKOUT_MINUTES   = 15;

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String email) {
        AttemptInfo info = attempts.get(normalise(email));
        if (info == null) return false;
        if (info.count >= MAX_ATTEMPTS) {
            if (LocalDateTime.now().isAfter(info.lockUntil)) {
                attempts.remove(normalise(email));
                return false;
            }
            return true;
        }
        return false;
    }

    public void recordFailure(String email) {
        AttemptInfo info = attempts.computeIfAbsent(normalise(email), k -> new AttemptInfo());
        info.count++;
        if (info.count >= MAX_ATTEMPTS) {
            info.lockUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
        }
    }

    public void reset(String email) {
        attempts.remove(normalise(email));
    }

    public int remainingAttempts(String email) {
        AttemptInfo info = attempts.get(normalise(email));
        if (info == null) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - info.count);
    }

    private String normalise(String email) {
        return email.toLowerCase().trim();
    }

    private static class AttemptInfo {
        int count = 0;
        LocalDateTime lockUntil = LocalDateTime.MAX;
    }
}
