package br.com.gym.flow.evolution.domain;

import br.com.gym.flow.history.domain.spi.WorkoutExecutionItemView;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvolutionCalculatorTest {

    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRAINING = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID BENCH = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID SQUAT = UUID.fromString("00000000-0000-0000-0000-0000000000e2");
    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-30T23:59:59Z");

    private static WorkoutExecutionView execution(final Instant startedAt, final WorkoutExecutionItemView... items) {
        return new WorkoutExecutionView(UUID.randomUUID(), STUDENT, TRAINING, startedAt, startedAt.plusSeconds(3600),
            null, List.of(items), startedAt);
    }

    private static WorkoutExecutionItemView item(final UUID exerciseId, final int sets, final int reps, final String load) {
        return new WorkoutExecutionItemView(exerciseId, sets, reps, load == null ? null : new BigDecimal(load), null);
    }

    @Test
    void givenNoExecutions_whenComputing_thenAllSeriesEmpty() {
        var report = EvolutionCalculator.compute(STUDENT, Granularity.WEEKLY, FROM, TO, List.of());

        assertThat(report.studentId()).isEqualTo(STUDENT);
        assertThat(report.granularity()).isEqualTo("WEEKLY");
        assertThat(report.frequency()).isEmpty();
        assertThat(report.volume()).isEmpty();
        assertThat(report.oneRepMaxByExercise()).isEmpty();
        assertThat(report.bodyweight()).isEmpty();
    }

    @Test
    void givenTwoExecutionsSameMonth_whenComputingMonthly_thenFrequencyAndVolumeAggregate() {
        // Given — two June executions; bench 4×10×40 = 1600 and 3×10×50 = 1500 -> 3100
        var june5 = execution(Instant.parse("2026-06-05T10:00:00Z"), item(BENCH, 4, 10, "40.00"));
        var june20 = execution(Instant.parse("2026-06-20T10:00:00Z"), item(BENCH, 3, 10, "50.00"));

        var report = EvolutionCalculator.compute(STUDENT, Granularity.MONTHLY, FROM, TO, List.of(june5, june20));

        // Then — one bucket "2026-06"
        assertThat(report.frequency()).singleElement()
            .satisfies(p -> {
                assertThat(p.bucket()).isEqualTo("2026-06");
                assertThat(p.workouts()).isEqualTo(2);
            });
        assertThat(report.volume()).singleElement()
            .satisfies(p -> assertThat(p.totalVolume()).isEqualByComparingTo("3100.00"));
    }

    @Test
    void givenLoadedExercise_whenComputing_thenEstimatesOneRepMaxByEpley() {
        // Given — bench 1×10×100; Epley 1RM = 100 × (1 + 10/30) = 133.33
        var execution = execution(Instant.parse("2026-06-05T10:00:00Z"), item(BENCH, 1, 10, "100.00"));

        var report = EvolutionCalculator.compute(STUDENT, Granularity.MONTHLY, FROM, TO, List.of(execution));

        assertThat(report.oneRepMaxByExercise()).singleElement()
            .satisfies(series -> {
                assertThat(series.exerciseId()).isEqualTo(BENCH);
                assertThat(series.points()).singleElement()
                    .satisfies(point -> {
                        assertThat(point.bucket()).isEqualTo("2026-06");
                        assertThat(point.estimatedOneRepMax()).isEqualByComparingTo("133.33");
                    });
            });
    }

    @Test
    void givenSameExerciseTwiceInBucket_whenComputing_thenKeepsBestOneRepMax() {
        // Given — two bench sessions in June: 10@100 (133.33) and 5@120 (140.00) -> keep 140.00
        var weaker = execution(Instant.parse("2026-06-05T10:00:00Z"), item(BENCH, 1, 10, "100.00"));
        var stronger = execution(Instant.parse("2026-06-25T10:00:00Z"), item(BENCH, 1, 5, "120.00"));

        var report = EvolutionCalculator.compute(STUDENT, Granularity.MONTHLY, FROM, TO, List.of(weaker, stronger));

        assertThat(report.oneRepMaxByExercise()).singleElement()
            .satisfies(series -> assertThat(series.points()).singleElement()
                .satisfies(point -> assertThat(point.estimatedOneRepMax()).isEqualByComparingTo("140.00")));
    }

    @Test
    void givenBodyweightItem_whenComputing_thenIgnoredInVolumeAndOneRepMax() {
        // Given — a single bodyweight movement (null load)
        var execution = execution(Instant.parse("2026-06-05T10:00:00Z"), item(SQUAT, 3, 15, null));

        var report = EvolutionCalculator.compute(STUDENT, Granularity.MONTHLY, FROM, TO, List.of(execution));

        // Then — counts for frequency, but contributes nothing to volume / 1RM
        assertThat(report.frequency()).singleElement().satisfies(p -> assertThat(p.workouts()).isEqualTo(1));
        assertThat(report.volume()).isEmpty();
        assertThat(report.oneRepMaxByExercise()).isEmpty();
    }

    @Test
    void givenExecutionsAcrossWeeks_whenComputingWeekly_thenBucketsAreIsoWeeksSorted() {
        // Given — two executions in different ISO weeks
        var early = execution(Instant.parse("2026-06-02T10:00:00Z"), item(BENCH, 4, 10, "40.00"));
        var later = execution(Instant.parse("2026-06-16T10:00:00Z"), item(BENCH, 4, 10, "40.00"));

        var report = EvolutionCalculator.compute(STUDENT, Granularity.WEEKLY, FROM, TO, List.of(later, early));

        // Then — two weekly buckets, ascending (input order does not matter)
        assertThat(report.frequency()).hasSize(2);
        assertThat(report.frequency().get(0).bucket()).isLessThan(report.frequency().get(1).bucket());
        assertThat(report.frequency().get(0).bucket()).startsWith("2026-W");
    }
}
