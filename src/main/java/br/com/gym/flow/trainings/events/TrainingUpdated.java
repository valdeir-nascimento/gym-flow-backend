package br.com.gym.flow.trainings.events;

import br.com.gym.flow.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Raised when a training is updated (RF-005). Carries the audit trail of the
 * change: who ({@code instructorId}), when ({@code occurredOn}) and what
 * ({@code changedFields}). Intended for the notification of the target student.
 */
public record TrainingUpdated(
    UUID id,
    Instant occurredOn,
    UUID trainingId,
    UUID studentId,
    UUID instructorId,
    List<String> changedFields
) implements DomainEvent {

    public static TrainingUpdated of(UUID trainingId, UUID studentId, UUID instructorId,
                                     List<String> changedFields, Instant occurredOn) {
        return new TrainingUpdated(UUID.randomUUID(), occurredOn, trainingId, studentId, instructorId,
            List.copyOf(changedFields));
    }
}
