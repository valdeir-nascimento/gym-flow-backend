package br.com.gym.flow.trainings.domain;

import br.com.gym.flow.shared.domain.AggregateRoot;
import br.com.gym.flow.trainings.events.TrainingCreated;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root of a personalized training (RF-004). Composed of one or more
 * {@link TrainingItem}s, it references the target student and the prescribing
 * instructor by id (no coupling to those aggregates). The aggregate owns its
 * items as an immutable list and guards the "has items" invariant on creation.
 */
@Getter
@Accessors(fluent = true)
public class Training extends AggregateRoot<TrainingId> {

    private final UUID studentId;
    private final UUID instructorId;
    private String name;
    private String objective;
    private TrainingPeriod period;
    private TrainingStatus status;
    private final List<TrainingItem> items;
    private final Instant createdAt;
    private Instant updatedAt;

    private Training(TrainingId id, UUID studentId, UUID instructorId, String name, String objective,
                     TrainingPeriod period, TrainingStatus status, List<TrainingItem> items,
                     Instant createdAt, Instant updatedAt) {
        super(id);
        this.studentId = studentId;
        this.instructorId = instructorId;
        this.name = name;
        this.objective = objective;
        this.period = period;
        this.status = status;
        this.items = List.copyOf(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Training create(UUID studentId, UUID instructorId, TrainingDraft draft, Clock clock) {
        if (draft.items().isEmpty()) {
            throw new IllegalArgumentException("a training must contain at least one item");
        }
        Instant now = Instant.now(clock);
        Training training = new Training(TrainingId.newId(), studentId, instructorId,
            draft.name(), draft.objective(), draft.period(), TrainingStatus.ACTIVE, draft.items(), now, now);
        training.registerEvent(TrainingCreated.of(training.id().value(), studentId, instructorId, now));
        return training;
    }

    public static Training hydrate(TrainingId id, UUID studentId, UUID instructorId, String name, String objective,
                                   TrainingPeriod period, TrainingStatus status, List<TrainingItem> items,
                                   Instant createdAt, Instant updatedAt) {
        return new Training(id, studentId, instructorId, name, objective, period, status, items, createdAt, updatedAt);
    }

    public boolean isActiveOn(LocalDate date) {
        return status.isActive() && period.covers(date);
    }
}
