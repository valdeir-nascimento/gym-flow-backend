package br.com.gym.flow.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public IssuedToken issueAccess(UUID userId, String role) {
        return issue(userId, role, TYPE_ACCESS, properties.accessTtl().toMillis());
    }

    public IssuedToken issueRefresh(UUID userId, String role) {
        return issue(userId, role, TYPE_REFRESH, properties.refreshTtl().toMillis());
    }

    private IssuedToken issue(UUID userId, String role, String type, long ttlMillis) {
        Instant now = Instant.now(clock);
        Instant exp = now.plusMillis(ttlMillis);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
            .id(jti)
            .subject(userId.toString())
            .issuer(properties.issuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claim(CLAIM_ROLE, role)
            .claim("type", type)
            .signWith(signingKey)
            .compact();
        return new IssuedToken(token, jti, exp);
    }

    public Optional<JwtClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String role = claims.get(CLAIM_ROLE, String.class);
            Instant exp = claims.getExpiration().toInstant();
            return Optional.of(new JwtClaims(userId, role, exp, claims.getId()));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, String tokenId, Instant expiresAt) {}
}
