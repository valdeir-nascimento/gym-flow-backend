package br.com.gym.flow.users.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record BondEnded(
    UUID id,
    Instant occurredOn,
    UUID bondId,
    UUID studentId,
    UUID instructorId
) implements DomainEvent {
    public static BondEnded of(UUID bondId, UUID studentId, UUID instructorId, Instant occurredOn) {
        return new BondEnded(UUID.randomUUID(), occurredOn, bondId, studentId, instructorId);
    }
}
