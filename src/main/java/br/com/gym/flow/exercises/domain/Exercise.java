package br.com.gym.flow.exercises.domain;

import br.com.gym.flow.exercises.events.ExerciseDeactivated;
import br.com.gym.flow.shared.domain.AggregateRoot;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root of the exercise catalog (RF-011). Guards its own lifecycle:
 * a created exercise is {@code ACTIVE}, can be edited, and can be deactivated
 * (kept for existing workouts but no longer offered for new ones). Name
 * uniqueness is a catalog-wide invariant enforced by the application layer
 * (and the DB), since it spans all exercises rather than a single aggregate.
 */
@Getter
@Accessors(fluent = true)
public class Exercise extends AggregateRoot<ExerciseId> {

    private String name;
    private MuscleGroup muscleGroup;
    private String description;
    private String equipment;
    private DifficultyLevel difficultyLevel;
    private String videoUrl;
    private String imageUrl;
    private ExerciseStatus status;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private Exercise(
        final ExerciseId id,
        final ExerciseDetails details,
        final ExerciseStatus status,
        final UUID createdBy,
        final Instant createdAt,
        final Instant updatedAt
    ) {
        super(id);
        applyDetails(details);
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Exercise create(ExerciseDetails details, UUID createdBy, Clock clock) {
        Instant now = Instant.now(clock);
        return new Exercise(ExerciseId.newId(), details, ExerciseStatus.ACTIVE, createdBy, now, now);
    }

    public static Exercise hydrate(
        final ExerciseId id,
        final ExerciseDetails details,
        final ExerciseStatus status,
        final UUID createdBy,
        final Instant createdAt,
        final Instant updatedAt
    ) {
        return new Exercise(id, details, status, createdBy, createdAt, updatedAt);
    }

    public void update(ExerciseDetails details, Clock clock) {
        applyDetails(details);
        this.updatedAt = Instant.now(clock);
    }

    public Result<Void> deactivate(Clock clock) {
        if (status == ExerciseStatus.INACTIVE) {
            return Result.failWith(ErrorCode.EXERCISE_ALREADY_INACTIVE);
        }
        Instant now = Instant.now(clock);
        this.status = ExerciseStatus.INACTIVE;
        this.updatedAt = now;
        registerEvent(ExerciseDeactivated.of(id().value(), now));
        return Result.ok();
    }

    public boolean isActive() {
        return status.isActive();
    }

    private void applyDetails(ExerciseDetails details) {
        this.name = details.name();
        this.muscleGroup = details.muscleGroup();
        this.description = details.description();
        this.equipment = details.equipment();
        this.difficultyLevel = details.difficultyLevel();
        this.videoUrl = details.videoUrl();
        this.imageUrl = details.imageUrl();
    }
}
