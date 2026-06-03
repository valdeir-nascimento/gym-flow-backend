package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.Notification;

/**
 * Provedor externo indisponível (timeout, circuito aberto, 5xx upstream).
 * Mapeado para HTTP 503 pelo {@link GlobalExceptionHandler}.
 */
public class ServiceUnavailableException extends NotificationException {
    public ServiceUnavailableException(Notification notification, Throwable cause) {
        super(notification);
        initCause(cause);
    }
}
