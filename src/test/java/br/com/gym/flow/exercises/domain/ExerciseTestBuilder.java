package br.com.gym.flow.exercises.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Test Data Builder for {@link Exercise}. {@code anExercise().build()} yields a
 * valid, ACTIVE exercise with no pending domain events (built via
 * {@link Exercise#hydrate}); each test overrides only the axis it exercises.
 * Public so the application/use-case and controller tests share one builder.
 */
public final class ExerciseTestBuilder {

    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);

    private ExerciseId id = ExerciseId.of(UUID.fromString("00000000-0000-0000-0000-000000000010"));
    private String name = "Supino Reto";
    private MuscleGroup muscleGroup = MuscleGroup.CHEST;
    private String description = "Exercício composto de empurrar para o peitoral.";
    private String equipment = "Barra";
    private DifficultyLevel difficultyLevel = DifficultyLevel.INTERMEDIATE;
    private String videoUrl = null;
    private String imageUrl = null;
    private ExerciseStatus status = ExerciseStatus.ACTIVE;
    private UUID createdBy = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    private Instant updatedAt = Instant.parse("2026-01-01T00:00:00Z");

    public static ExerciseTestBuilder anExercise() {
        return new ExerciseTestBuilder();
    }

    public ExerciseTestBuilder withId(ExerciseId id) {
        this.id = id;
        return this;
    }

    public ExerciseTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ExerciseTestBuilder withStatus(ExerciseStatus status) {
        this.status = status;
        return this;
    }

    public ExerciseDetails details() {
        return new ExerciseDetails(name, muscleGroup, description, equipment, difficultyLevel, videoUrl, imageUrl);
    }

    public Exercise build() {
        return Exercise.hydrate(id, details(), status, createdBy, createdAt, updatedAt);
    }
}
