package br.com.gym.flow.trainings.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a training is created (RF-004). Intended for the notification of
 * the target student.
 */
public record TrainingCreated(
    UUID id,
    Instant occurredOn,
    UUID trainingId,
    UUID studentId,
    UUID instructorId
) implements DomainEvent {

    public static TrainingCreated of(UUID trainingId, UUID studentId, UUID instructorId, Instant occurredOn) {
        return new TrainingCreated(UUID.randomUUID(), occurredOn, trainingId, studentId, instructorId);
    }
}
