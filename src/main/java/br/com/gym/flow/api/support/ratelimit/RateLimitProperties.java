package br.com.gym.flow.api.support.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(boolean enabled, List<Rule> rules) {

    public RateLimitProperties {
        if (rules == null) rules = List.of();
    }

    public record Rule(
        List<String> paths,
        List<String> methods,
        long capacity,
        long refillTokens,
        Duration refillPeriod
    ) {
        public Rule {
            if (methods == null || methods.isEmpty()) methods = List.of("GET", "POST", "PUT", "PATCH", "DELETE");
        }
    }
}
