package br.com.gym.flow.exercises.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ExerciseDeactivated(UUID id, Instant occurredOn, UUID exerciseId) implements DomainEvent {

    public static ExerciseDeactivated of(UUID exerciseId, Instant occurredOn) {
        return new ExerciseDeactivated(UUID.randomUUID(), occurredOn, exerciseId);
    }
}
