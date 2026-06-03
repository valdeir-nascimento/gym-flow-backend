package br.com.gym.flow.authentication.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String from, String senderName, String baseUrl) {
    public MailProperties {
        if (from == null || from.isBlank()) from = "noreply@wsfitness.local";
        if (senderName == null || senderName.isBlank()) senderName = "WS Fitness";
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://localhost:8080";
    }
}
