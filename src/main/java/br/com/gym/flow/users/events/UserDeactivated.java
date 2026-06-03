package br.com.gym.flow.users.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserDeactivated(UUID id, Instant occurredOn, UUID userId, String role) implements DomainEvent {
    public static UserDeactivated of(UUID userId, String role, Instant occurredOn) {
        return new UserDeactivated(UUID.randomUUID(), occurredOn, userId, role);
    }
}
