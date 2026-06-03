package br.com.gym.flow.trainings.domain;

import br.com.gym.flow.shared.domain.AggregateRoot;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.events.TrainingCreated;
import br.com.gym.flow.trainings.events.TrainingUpdated;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private List<TrainingItem> items;
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

    /** Ownership rule (RNF-001): a training belongs to the instructor who created it. */
    public boolean isOwnedBy(UUID userId) {
        return instructorId.equals(userId);
    }

    /**
     * Applies validated changes (RF-005). Rejects edits to an ARCHIVED training,
     * keeps the "≥ 1 item" invariant, and records which top-level fields changed
     * in {@link TrainingUpdated}. A null {@code newStatus} leaves the status as-is.
     */
    public Result<Void> update(TrainingDraft draft, TrainingStatus newStatus, Clock clock) {
        if (status == TrainingStatus.ARCHIVED) {
            return Result.failWith(ErrorCode.TRAINING_ARCHIVED);
        }
        if (draft.items().isEmpty()) {
            throw new IllegalArgumentException("a training must contain at least one item");
        }
        TrainingStatus targetStatus = newStatus != null ? newStatus : status;

        List<String> changed = new ArrayList<>();
        if (!name.equals(draft.name())) changed.add("name");
        if (!Objects.equals(objective, draft.objective())) changed.add("objective");
        if (!period.equals(draft.period())) changed.add("period");
        if (!items.equals(draft.items())) changed.add("items");
        if (status != targetStatus) changed.add("status");

        this.name = draft.name();
        this.objective = draft.objective();
        this.period = draft.period();
        this.items = List.copyOf(draft.items());
        this.status = targetStatus;
        Instant now = Instant.now(clock);
        this.updatedAt = now;
        registerEvent(TrainingUpdated.of(id().value(), studentId, instructorId, changed, now));
        return Result.ok();
    }

    public boolean isActiveOn(LocalDate date) {
        return status.isActive() && period.covers(date);
    }
}
