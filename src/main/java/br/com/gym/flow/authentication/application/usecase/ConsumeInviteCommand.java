package br.com.gym.flow.authentication.application.usecase;

public record ConsumeInviteCommand(String rawToken, String newPassword, String passwordConfirmation, String ipAddress) {}
