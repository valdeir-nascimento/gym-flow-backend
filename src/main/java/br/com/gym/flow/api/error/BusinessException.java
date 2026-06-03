package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.Notification;

/**
 * Boundary exception thrown by controllers when a {@code Result.Failure}
 * crosses into the HTTP layer. The actual HTTP status is decided by
 * {@link GlobalExceptionHandler} from the first error's {@code ErrorCategory},
 * so adding a new {@code ErrorCode} requires no controller change.
 */
public class BusinessException extends NotificationException {
    public BusinessException(Notification notification) {
        super(notification);
    }
}
