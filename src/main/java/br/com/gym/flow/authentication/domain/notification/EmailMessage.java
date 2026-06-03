package br.com.gym.flow.authentication.domain.notification;

import java.util.Map;

public record EmailMessage(String to, String subject, String templateName, Map<String, Object> variables) {}
