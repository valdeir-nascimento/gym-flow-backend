package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.ErrorCategory;
import org.springframework.http.HttpStatus;

/**
 * Single source of truth for translating an {@link ErrorCategory} (domain
 * concept) to an {@link HttpStatus} (HTTP boundary concept). Keeps the
 * domain free of Spring HTTP dependencies.
 */
final class ErrorCategoryHttpStatus {

    private ErrorCategoryHttpStatus() {}

    static HttpStatus of(ErrorCategory category) {
        return switch (category) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case INFRASTRUCTURE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
