package br.com.gym.flow.api.support.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * In-memory IP-keyed rate limiter. One Caffeine cache per rule keyed by client
 * IP, holding a Bucket4j bucket per remote address. Returns 429 with Retry-After
 * when the bucket is exhausted.
 */
@Component
@ConditionalOnProperty(prefix = "rate-limit", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RateLimitProperties.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final List<CompiledRule> rules;

    public RateLimitFilter(RateLimitProperties props) {
        this.rules = props.rules().stream().map(CompiledRule::compile).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientKey = clientKey(request);

        for (CompiledRule rule : rules) {
            if (!rule.matches(path, method)) continue;
            Bucket bucket = rule.bucketFor(clientKey);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
                response.setStatus(429); // Too Many Requests — not a constant in jakarta.servlet
                response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"too many requests; retry after " +
                    retryAfterSeconds + "s\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private static final class CompiledRule {
        private final List<String> paths;
        private final List<String> methods;
        private final Bandwidth bandwidth;
        private final Cache<String, Bucket> buckets;

        private CompiledRule(List<String> paths, List<String> methods, Bandwidth bandwidth, Cache<String, Bucket> buckets) {
            this.paths = paths;
            this.methods = methods;
            this.bandwidth = bandwidth;
            this.buckets = buckets;
        }

        static CompiledRule compile(RateLimitProperties.Rule rule) {
            Bandwidth bandwidth = Bandwidth.classic(rule.capacity(),
                Refill.greedy(rule.refillTokens(), rule.refillPeriod()));
            Cache<String, Bucket> cache = Caffeine.newBuilder()
                .expireAfterAccess(rule.refillPeriod().multipliedBy(2))
                .maximumSize(10_000)
                .build();
            return new CompiledRule(rule.paths(), rule.methods(), bandwidth, cache);
        }

        boolean matches(String path, String method) {
            if (!methods.contains(method)) return false;
            for (String p : paths) {
                if (path.equals(p)) return true;
                if (p.endsWith("/**") && path.startsWith(p.substring(0, p.length() - 2))) return true;
            }
            return false;
        }

        Bucket bucketFor(String clientKey) {
            return buckets.get(clientKey, k -> Bucket.builder().addLimit(bandwidth).build());
        }

        @SuppressWarnings("unused")
        Duration period() { return Duration.ZERO; }
    }
}
