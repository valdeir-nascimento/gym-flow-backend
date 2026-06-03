package br.com.gym.flow.authentication.domain.notification;

public interface EmailSender {
    void send(EmailMessage message);
}
