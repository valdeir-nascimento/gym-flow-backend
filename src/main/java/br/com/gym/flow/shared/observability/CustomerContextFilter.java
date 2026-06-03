package br.com.gym.flow.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs after the JWT authentication filter has populated the SecurityContext.
 * Pulls the authenticated principal name (customer/admin id) into MDC so log
 * lines emitted by controllers and use cases carry the customerId tag.
 *
 * Spring registers OncePerRequestFilter beans automatically. Order is
 * intentionally not specified — the JWT filter is wired into the
 * SecurityFilterChain ahead of the servlet filter chain, so by the time this
 * filter runs the SecurityContext is already populated.
 */
@Component
public class CustomerContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            MDC.put(MdcContext.MDC_CUSTOMER_ID, auth.getName());
        }
        chain.doFilter(request, response);
        // RequestContextFilter (HIGHEST_PRECEDENCE) clears MDC at the outer scope.
    }
}
