package br.com.gym.flow.users.domain.spi;

import java.time.Instant;
import java.util.UUID;

public record HealthConsentView(UUID id, UUID studentId, UUID grantedBy, Instant grantedAt) {}
