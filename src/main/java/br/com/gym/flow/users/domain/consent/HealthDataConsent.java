package br.com.gym.flow.users.domain.consent;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a student's consent to the processing of their health data
 * (RNF-008/LGPD), a prerequisite for registering an anamnesis (RF-017).
 * Immutable; one active consent per student is sufficient.
 */
public record HealthDataConsent(UUID id, UUID studentId, UUID grantedBy, Instant grantedAt) {

    public static HealthDataConsent grant(final UUID studentId, final UUID grantedBy, final Clock clock) {
        return new HealthDataConsent(UUID.randomUUID(), studentId, grantedBy, Instant.now(clock));
    }
}
