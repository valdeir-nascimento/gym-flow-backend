package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.Notification;

public class UnauthorizedException extends NotificationException {
    public UnauthorizedException(Notification notification) {
        super(notification);
    }
}
