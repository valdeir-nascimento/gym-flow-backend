package br.com.gym.flow.users.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when an anamnesis revision is registered (RF-017). Intended for the
 * {@code evolution} module (body-weight series) and the {@code trainings} module
 * (contraindicated exercises).
 */
public record AnamnesisRegistered(
    UUID id,
    Instant occurredOn,
    UUID anamnesisId,
    UUID studentId,
    int version,
    UUID createdBy
) implements DomainEvent {

    public static AnamnesisRegistered of(
        final UUID anamnesisId,
        final UUID studentId,
        final int version,
        final UUID createdBy,
        final Instant occurredOn
    ) {
        return new AnamnesisRegistered(UUID.randomUUID(), occurredOn, anamnesisId, studentId, version, createdBy);
    }
}
