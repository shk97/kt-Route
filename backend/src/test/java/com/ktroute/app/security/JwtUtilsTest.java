package com.ktroute.app.security;

import com.ktroute.app.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @InjectMocks
    private JwtUtils jwtUtils;

    private static final String SECRET =
            "TestSecretKeyForUnitTestsOnly1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdef";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);
    }

    @Test
    void generateToken_returnsNonNull() {
        String token = jwtUtils.generateToken("user@test.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void getEmailFromToken_returnsCorrectEmail() {
        String token = jwtUtils.generateToken("user@test.com");
        assertThat(jwtUtils.getEmailFromToken(token)).isEqualTo("user@test.com");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtils.generateToken("user@test.com");
        assertThat(jwtUtils.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertThat(jwtUtils.validateToken("this.is.not.valid")).isFalse();
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtUtils.validateToken("")).isFalse();
    }

    @Test
    void getExpiry_returnsNonNull() {
        String token = jwtUtils.generateToken("user@test.com");
        assertThat(jwtUtils.getExpiry(token)).isNotNull();
    }

}
