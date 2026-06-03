package br.com.gym.flow.users.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileUpdated(
    UUID id,
    Instant occurredOn,
    UUID userId,
    List<String> changedFields
) implements DomainEvent {
    public static UserProfileUpdated of(UUID userId, List<String> changedFields, Instant occurredOn) {
        return new UserProfileUpdated(UUID.randomUUID(), occurredOn, userId, List.copyOf(changedFields));
    }
}
