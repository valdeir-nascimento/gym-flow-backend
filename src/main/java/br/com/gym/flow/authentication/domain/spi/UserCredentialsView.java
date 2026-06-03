package br.com.gym.flow.authentication.domain.spi;

import java.util.UUID;

public record UserCredentialsView(
    UUID userId,
    String email,
    String passwordHash,
    String role,
    String status
) {}
