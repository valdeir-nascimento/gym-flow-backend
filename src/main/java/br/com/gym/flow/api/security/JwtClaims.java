package br.com.gym.flow.api.security;

import java.time.Instant;
import java.util.UUID;

public record JwtClaims(UUID userId, String role, Instant expiresAt, String tokenId) {}
