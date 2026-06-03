package br.com.gym.flow.authentication.application.usecase;

public record LoginCommand(String email, String rawPassword, String ipAddress) {}
