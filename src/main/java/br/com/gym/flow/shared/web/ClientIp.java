package br.com.gym.flow.shared.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the originating client IP from a request, honouring a proxy's
 * {@code X-Forwarded-For} (first hop) and falling back to the socket address.
 *
 * <p>Lives in the OPEN {@code shared} kernel so any module can depend on it —
 * previously in {@code api.support}, which is a named interface not exposed to
 * feature modules (the dependency was an architecture violation).
 */
public final class ClientIp {

    private ClientIp() {}

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
