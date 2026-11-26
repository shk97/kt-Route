package com.ktroute.app.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication auth) {
        return buildToken(((UserDetails) auth.getPrincipal()).getUsername(), jwtExpirationMs);
    }

    public String generateToken(String email) {
        return buildToken(email, jwtExpirationMs);
    }

    private String buildToken(String subject, long ttlMs) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return claims(token).getSubject();
    }

    public LocalDateTime getExpiry(String token) {
        Date exp = claims(token).getExpiration();
        return exp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e)  { log.warn("Invalid JWT: {}", e.getMessage()); }
        catch (ExpiredJwtException e)       { log.warn("JWT expired: {}", e.getMessage()); }
        catch (UnsupportedJwtException e)   { log.warn("JWT unsupported: {}", e.getMessage()); }
        catch (IllegalArgumentException e)  { log.warn("JWT claims empty: {}", e.getMessage()); }
        return false;
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody();
    }
}
