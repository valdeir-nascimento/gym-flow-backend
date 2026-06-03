package br.com.gym.flow.authentication.application.usecase;

import java.time.Instant;
import java.util.UUID;

public record TokenPairView(
    String accessToken,
    String refreshToken,
    Instant accessExpiresAt,
    Instant refreshExpiresAt,
    UUID userId,
    String role
) {}
