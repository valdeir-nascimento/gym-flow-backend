package br.com.gym.flow.authentication.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.password-recovery")
public record PasswordRecoveryProperties(Duration ttl) {
    public PasswordRecoveryProperties {
        if (ttl == null) ttl = Duration.ofHours(1);
    }
}
