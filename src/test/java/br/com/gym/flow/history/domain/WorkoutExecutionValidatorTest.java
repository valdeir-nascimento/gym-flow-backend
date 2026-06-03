package br.com.gym.flow.history.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutExecutionValidatorTest {

    private static final Instant START = Instant.parse("2026-06-01T11:00:00Z");
    private static final Instant END = Instant.parse("2026-06-01T11:45:00Z");
    private static final UUID EXERCISE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    private static ExecutedExercise item() {
        return new ExecutedExercise(EXERCISE, 4, 10, new BigDecimal("40.00"), null);
    }

    @Test
    void givenValidInput_whenValidating_thenSucceedsAndTrimsBlankNotesToNull() {
        var result = WorkoutExecutionValidator.validate(START, END, "   ", List.of(item()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().notes()).isNull();
        assertThat(result.getOrThrow().items()).hasSize(1);
    }

    @Test
    void givenNullTimestamps_whenValidating_thenFailsValidation() {
        var result = WorkoutExecutionValidator.validate(null, null, null, List.of(item()));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?>) result).notification().hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
    }

    @Test
    void givenNoItems_whenValidating_thenFailsValidation() {
        var result = WorkoutExecutionValidator.validate(START, END, null, List.of());

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?>) result).notification().hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
    }

    @Test
    void givenItemWithNonPositiveNumbers_whenValidating_thenFailsValidation() {
        var bad = new ExecutedExercise(EXERCISE, 0, -1, new BigDecimal("-5.00"), null);
        var result = WorkoutExecutionValidator.validate(START, END, null, List.of(bad));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?>) result).notification().hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
    }
}
