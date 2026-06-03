package br.com.gym.flow.authentication.application.usecase;

public record ResetPasswordCommand(String rawToken, String newPassword, String passwordConfirmation) {}
