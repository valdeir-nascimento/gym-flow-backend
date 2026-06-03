package br.com.gym.flow.authentication.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.login-throttle")
public record LoginThrottleProperties(int maxAttempts, Duration window, Duration blockDuration) {
    public LoginThrottleProperties {
        if (maxAttempts <= 0) maxAttempts = 5;
        if (window == null) window = Duration.ofMinutes(15);
        if (blockDuration == null) blockDuration = Duration.ofMinutes(15);
    }
}
