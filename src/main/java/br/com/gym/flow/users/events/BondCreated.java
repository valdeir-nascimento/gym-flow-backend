package br.com.gym.flow.users.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record BondCreated(
    UUID id,
    Instant occurredOn,
    UUID bondId,
    UUID studentId,
    UUID instructorId
) implements DomainEvent {
    public static BondCreated of(UUID bondId, UUID studentId, UUID instructorId, Instant occurredOn) {
        return new BondCreated(UUID.randomUUID(), occurredOn, bondId, studentId, instructorId);
    }
}
