package br.com.gym.flow.history.domain;

import br.com.gym.flow.history.events.WorkoutExecutionRegistered;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkoutExecutionTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRAINING = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID EXERCISE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static ExecutedExercise item() {
        return new ExecutedExercise(EXERCISE, 4, 10, new BigDecimal("40.00"), "ok");
    }

    private static WorkoutExecutionDraft draft(final Instant startedAt, final Instant finishedAt) {
        return new WorkoutExecutionDraft(startedAt, finishedAt, "felt good", List.of(item()));
    }

    @Test
    void givenValidDraft_whenRegistering_thenImmutableWithItemsAndEvent() {
        // Given — a session entirely in the past
        Instant start = NOW.minus(Duration.ofHours(1));
        Instant end = NOW.minus(Duration.ofMinutes(30));

        // When
        var result = WorkoutExecution.register(STUDENT, TRAINING, draft(start, end), CLOCK);

        // Then
        assertThat(result.isSuccess()).isTrue();
        WorkoutExecution execution = result.getOrThrow();
        assertThat(execution.studentId()).isEqualTo(STUDENT);
        assertThat(execution.trainingId()).isEqualTo(TRAINING);
        assertThat(execution.items()).hasSize(1);
        assertThat(execution.registeredAt()).isEqualTo(NOW);
        assertThat(execution.pullDomainEvents()).singleElement().isInstanceOf(WorkoutExecutionRegistered.class);
    }

    @Test
    void givenFutureTimestamp_whenRegistering_thenFailsAsValidation() {
        // Given — finishedAt in the future
        Instant start = NOW.minus(Duration.ofMinutes(30));
        Instant end = NOW.plus(Duration.ofMinutes(30));

        // When
        var result = WorkoutExecution.register(STUDENT, TRAINING, draft(start, end), CLOCK);

        // Then — VALIDATION (400)
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_FUTURE_DATETIME)).isTrue();
    }

    @Test
    void givenEndBeforeStart_whenRegistering_thenFailsAsBusinessRule() {
        // Given
        Instant start = NOW.minus(Duration.ofMinutes(30));
        Instant end = NOW.minus(Duration.ofHours(1));

        // When
        var result = WorkoutExecution.register(STUDENT, TRAINING, draft(start, end), CLOCK);

        // Then — BUSINESS_RULE (422)
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_END_BEFORE_START)).isTrue();
    }

    @Test
    void givenNoItems_whenRegistering_thenThrows() {
        Instant start = NOW.minus(Duration.ofHours(1));
        var emptyDraft = new WorkoutExecutionDraft(start, NOW, null, List.of());
        assertThatThrownBy(() -> WorkoutExecution.register(STUDENT, TRAINING, emptyDraft, CLOCK))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
