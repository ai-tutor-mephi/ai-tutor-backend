package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwtService {
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    public String generateAccessToken(UserDetails user) {
        return buildToken(user, props.getAccessExpiration());
    }

    public String generateRefreshToken(UserDetails user) {
        return buildToken(user, props.getRefreshExpiration());
    }

    public String extractUsername(String token) {
        return getAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails user) {
        String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isExpired(token) && isIssuedByUs(token);
    }

    private String buildToken(UserDetails user, long ttlSeconds) {
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(props.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claims(Map.of("roles", roles))
                .signWith(getKey())
                .compact();
    }

    private boolean isExpired(String token) {
        return getAllClaims(token).getExpiration().before(new Date());
    }

    private boolean isIssuedByUs(String token) {
        Claims c = getAllClaims(token);
        String iss = c.getIssuer();
        return iss != null && iss.equals(props.getIssuer());
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
