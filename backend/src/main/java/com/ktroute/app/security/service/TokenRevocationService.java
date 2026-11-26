package com.ktroute.app.security.service;

import com.ktroute.app.entity.RevokedToken;
import com.ktroute.app.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void revoke(String rawToken, LocalDateTime tokenExpiry) {
        String hash = sha256Hex(rawToken);
        if (!revokedTokenRepository.existsByTokenHash(hash)) {
            revokedTokenRepository.save(RevokedToken.builder()
                    .tokenHash(hash)
                    .expiresAt(tokenExpiry)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String rawToken) {
        return revokedTokenRepository.existsByTokenHash(sha256Hex(rawToken));
    }

    /** Purge expired revoked-token rows nightly so the table stays small. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        revokedTokenRepository.deleteExpired(LocalDateTime.now());
        log.info("Purged expired revoked-token rows");
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 64);
        } catch (Exception e) {
            throw new RuntimeException("Hash failed", e);
        }
    }
}
