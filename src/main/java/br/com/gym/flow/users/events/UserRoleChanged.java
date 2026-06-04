package br.com.gym.flow.users.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when an administrator changes a user's role (RF-012). Carries the audit
 * trail (previous → new role) for the change.
 */
public record UserRoleChanged(
    UUID id,
    Instant occurredOn,
    UUID userId,
    String previousRole,
    String newRole
) implements DomainEvent {

    public static UserRoleChanged of(UUID userId, String previousRole, String newRole, Instant occurredOn) {
        return new UserRoleChanged(UUID.randomUUID(), occurredOn, userId, previousRole, newRole);
    }
}
