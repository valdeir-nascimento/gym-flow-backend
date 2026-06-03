package br.com.gym.flow.trainings.domain;

import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.NotificationError;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingValidatorTest {

    private static final UUID EXERCISE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final LocalDate START = LocalDate.of(2026, 6, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 31);

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static TrainingItem validItem() {
        return new TrainingItem(EXERCISE, 4, 10, new BigDecimal("40.00"), 60);
    }

    @Test
    void givenValidInput_whenValidating_thenTrimsNameAndBuildsDraft() {
        // When
        var result = TrainingValidator.validate("  Treino A  ", " Hipertrofia ", START, END, List.of(validItem()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        var draft = result.getOrThrow();
        assertThat(draft.name()).isEqualTo("Treino A");
        assertThat(draft.objective()).isEqualTo("Hipertrofia");
        assertThat(draft.period().startDate()).isEqualTo(START);
        assertThat(draft.items()).hasSize(1);
    }

    @Test
    void givenBlankNameAndNoStartDate_whenValidating_thenFailsOnBothFields() {
        // When
        var result = TrainingValidator.validate("  ", null, null, END, List.of(validItem()));

        // Then
        assertThat(failureOf(result).errors()).extracting(NotificationError::field).contains("name", "startDate");
    }

    @Test
    void givenEndBeforeStart_whenValidating_thenFailsOnEndDate() {
        // When
        var result = TrainingValidator.validate("Treino A", null, START, START.minusDays(1), List.of(validItem()));

        // Then
        assertThat(failureOf(result).errors()).extracting(NotificationError::field).contains("endDate");
    }

    @Test
    void givenNoItems_whenValidating_thenFailsOnItems() {
        // When — RF-004: a training must contain at least one exercise
        var result = TrainingValidator.validate("Treino A", null, START, END, List.of());

        // Then
        assertThat(failureOf(result).errors()).extracting(NotificationError::field).contains("items");
    }

    @Test
    void givenItemWithInvalidNumbers_whenValidating_thenFailsOnThoseItemFields() {
        // When — sets 0 and negative rest
        var badItem = new TrainingItem(EXERCISE, 0, 10, null, -5);
        var result = TrainingValidator.validate("Treino A", null, START, END, List.of(badItem));

        // Then
        assertThat(failureOf(result).errors())
            .extracting(NotificationError::field)
            .contains("items[0].sets", "items[0].restSeconds");
    }
}
