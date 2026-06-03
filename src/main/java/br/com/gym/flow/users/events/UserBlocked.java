package br.com.gym.flow.users.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserBlocked(UUID id, Instant occurredOn, UUID userId) implements DomainEvent {
    public static UserBlocked of(UUID userId, Instant occurredOn) {
        return new UserBlocked(UUID.randomUUID(), occurredOn, userId);
    }
}
