package br.com.gym.flow.users.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserActivated(UUID id, Instant occurredOn, UUID userId) implements DomainEvent {
    public static UserActivated of(UUID userId, Instant occurredOn) {
        return new UserActivated(UUID.randomUUID(), occurredOn, userId);
    }
}
