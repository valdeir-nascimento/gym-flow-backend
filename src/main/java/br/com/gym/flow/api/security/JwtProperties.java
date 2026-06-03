package br.com.gym.flow.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    Duration accessTtl,
    Duration refreshTtl,
    String issuer
) {
    public JwtProperties {
        if (accessTtl == null) accessTtl = Duration.ofMinutes(15);
        if (refreshTtl == null) refreshTtl = Duration.ofDays(7);
        if (issuer == null || issuer.isBlank()) issuer = "ws-fitness";
    }
}
