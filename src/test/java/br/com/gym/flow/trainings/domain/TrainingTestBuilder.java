package br.com.gym.flow.trainings.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Test Data Builder for {@link Training} (built via {@link Training#hydrate}, so
 * no pending events). Defaults form a valid ACTIVE training with one item.
 */
public final class TrainingTestBuilder {

    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    public static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID INSTRUCTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    private TrainingId id = TrainingId.of(UUID.fromString("00000000-0000-0000-0000-000000000010"));
    private UUID studentId = STUDENT_ID;
    private UUID instructorId = INSTRUCTOR_ID;
    private String name = "Treino A";
    private String objective = "Hipertrofia";
    private TrainingPeriod period = new TrainingPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));
    private TrainingStatus status = TrainingStatus.ACTIVE;
    private List<TrainingItem> items = List.of(new TrainingItem(EXERCISE_ID, 4, 10, new BigDecimal("40.00"), 60));
    private final Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    private final Instant updatedAt = Instant.parse("2026-01-01T00:00:00Z");

    public static TrainingTestBuilder aTraining() {
        return new TrainingTestBuilder();
    }

    public TrainingTestBuilder withId(TrainingId id) {
        this.id = id;
        return this;
    }

    public TrainingTestBuilder withStatus(TrainingStatus status) {
        this.status = status;
        return this;
    }

    public TrainingTestBuilder withPeriod(TrainingPeriod period) {
        this.period = period;
        return this;
    }

    public TrainingDraft draft() {
        return new TrainingDraft(name, objective, period, items);
    }

    public Training build() {
        return Training.hydrate(id, studentId, instructorId, name, objective, period, status, items, createdAt, updatedAt);
    }
}
