package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.Notification;

public class NotFoundException extends NotificationException {
    public NotFoundException(Notification notification) {
        super(notification);
    }
}
