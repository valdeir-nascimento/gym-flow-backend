package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.Notification;

public class NotificationException extends RuntimeException {
    private final Notification notification;

    public NotificationException(Notification notification) {
        super("notification has errors");
        this.notification = notification;
    }

    public Notification notification() {
        return notification;
    }
}
