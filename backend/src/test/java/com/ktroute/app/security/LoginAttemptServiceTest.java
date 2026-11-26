package com.ktroute.app.security;

import com.ktroute.app.security.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    void newEmail_notLocked() {
        assertThat(service.isLocked("user@test.com")).isFalse();
    }

    @Test
    void remainingAttempts_newEmail_returnsFive() {
        assertThat(service.remainingAttempts("user@test.com")).isEqualTo(5);
    }

    @Test
    void recordFailure_once_notYetLocked() {
        service.recordFailure("user@test.com");

        assertThat(service.isLocked("user@test.com")).isFalse();
        assertThat(service.remainingAttempts("user@test.com")).isEqualTo(4);
    }

    @Test
    void recordFailure_fiveTimes_locksAccount() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("user@test.com");
        }

        assertThat(service.isLocked("user@test.com")).isTrue();
        assertThat(service.remainingAttempts("user@test.com")).isEqualTo(0);
    }

    @Test
    void reset_afterFailures_unlocksAccount() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("user@test.com");
        }
        assertThat(service.isLocked("user@test.com")).isTrue();

        service.reset("user@test.com");

        assertThat(service.isLocked("user@test.com")).isFalse();
        assertThat(service.remainingAttempts("user@test.com")).isEqualTo(5);
    }

    @Test
    void emailNormalisation_caseInsensitive() {
        service.recordFailure("USER@TEST.COM");

        assertThat(service.remainingAttempts("user@test.com")).isEqualTo(4);
    }

    @Test
    void emailNormalisation_trailingSpaces() {
        service.recordFailure("  user@test.com  ");

        assertThat(service.remainingAttempts("user@test.com")).isEqualTo(4);
    }

    @Test
    void recordFailure_fourTimes_oneLockoutRemaining() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("user@test.com");
        }

        assertThat(service.isLocked("user@test.com")).isFalse();
        assertThat(service.remainingAttempts("user@test.com")).isEqualTo(1);
    }
}
