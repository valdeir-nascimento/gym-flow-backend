package br.com.gym.flow.users.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserRegistered(
    UUID id,
    Instant occurredOn,
    UUID userId,
    String email,
    String role,
    String name,
    UUID createdBy,
    String createdByRole
) implements DomainEvent {

    public static UserRegistered of(
        UUID userId,
        String email,
        String role,
        String name,
        UUID createdBy,
        String createdByRole,
        Instant occurredOn) {
        return new UserRegistered(UUID.randomUUID(), occurredOn, userId, email, role, name, createdBy, createdByRole);
    }
}
