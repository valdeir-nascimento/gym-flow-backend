package br.com.gym.flow.shared.domain;

/**
 * Classifies how a domain error should be surfaced at the API boundary,
 * without coupling the domain to any HTTP framework. The translation to
 * an HTTP status lives at {@code api.error}.
 */
public enum ErrorCategory {
    VALIDATION,       // 400 — syntactic
    UNAUTHORIZED,     // 401 — bad credentials, expired token
    FORBIDDEN,        // 403 — authenticated but not allowed
    NOT_FOUND,        // 404
    CONFLICT,         // 409 — uniqueness, duplicate state
    BUSINESS_RULE,    // 422 — semantic invariant violated
    INFRASTRUCTURE    // 503 — downstream failure
}
