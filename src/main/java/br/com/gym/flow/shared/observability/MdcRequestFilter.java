package br.com.gym.flow.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates MDC for each incoming HTTP request:
 *   - requestId: from X-Request-Id header if present, else generated UUID;
 *     echoed back on the response.
 *   - correlationId: from X-Correlation-Id header if present.
 *   - customerId: populated by {@link CustomerContextFilter} after JWT auth runs.
 */
@Component("mdcRequestFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcRequestFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        String correlationId = request.getHeader(HEADER_CORRELATION_ID);

        try {
            MDC.put(MdcContext.MDC_REQUEST_ID, requestId);
            if (correlationId != null && !correlationId.isBlank()) {
                MDC.put(MdcContext.MDC_CORRELATION_ID, correlationId);
            }
            response.setHeader(HEADER_REQUEST_ID, requestId);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
