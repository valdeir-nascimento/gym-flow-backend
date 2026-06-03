package br.com.gym.flow.trainings.domain;

import br.com.gym.flow.trainings.events.TrainingCreated;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INSTRUCTOR = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EXERCISE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    private static TrainingDraft draftWith(List<TrainingItem> items) {
        return new TrainingDraft("Treino A", "Hipertrofia",
            new TrainingPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31)), items);
    }

    private static TrainingItem item() {
        return new TrainingItem(EXERCISE, 4, 10, new BigDecimal("40.00"), 60);
    }

    @Test
    void givenValidDraft_whenCreating_thenActiveWithItemsAndEvent() {
        // When
        var training = Training.create(STUDENT, INSTRUCTOR, draftWith(List.of(item())), CLOCK);

        // Then
        assertThat(training.status()).isEqualTo(TrainingStatus.ACTIVE);
        assertThat(training.studentId()).isEqualTo(STUDENT);
        assertThat(training.instructorId()).isEqualTo(INSTRUCTOR);
        assertThat(training.items()).hasSize(1);
        assertThat(training.createdAt()).isEqualTo(NOW);
        assertThat(training.pullDomainEvents()).singleElement().isInstanceOf(TrainingCreated.class);
    }

    @Test
    void givenDraftWithoutItems_whenCreating_thenThrows() {
        // The "at least one exercise" invariant is guarded by the aggregate
        assertThatThrownBy(() -> Training.create(STUDENT, INSTRUCTOR, draftWith(List.of()), CLOCK))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenActiveTrainingWithinPeriod_whenCheckingActiveOnDate_thenTrue() {
        // Given
        var training = Training.create(STUDENT, INSTRUCTOR, draftWith(List.of(item())), CLOCK);

        // When / Then
        assertThat(training.isActiveOn(LocalDate.of(2026, 7, 1))).isTrue();   // inside [Jun 1, Aug 31]
        assertThat(training.isActiveOn(LocalDate.of(2026, 9, 1))).isFalse();  // after the window
    }
}
