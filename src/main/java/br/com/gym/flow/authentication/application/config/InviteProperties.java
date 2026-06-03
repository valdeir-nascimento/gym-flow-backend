package br.com.gym.flow.authentication.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.invite")
public record InviteProperties(Duration ttl) {
    public InviteProperties {
        if (ttl == null) ttl = Duration.ofHours(72);
    }
}
