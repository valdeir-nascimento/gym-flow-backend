package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.Notification;

public class ConflictException extends NotificationException {
    public ConflictException(Notification notification) {
        super(notification);
    }
}
