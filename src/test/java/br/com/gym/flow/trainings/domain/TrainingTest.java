package br.com.gym.flow.trainings.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.events.TrainingCreated;
import br.com.gym.flow.trainings.events.TrainingUpdated;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static br.com.gym.flow.trainings.domain.TrainingTestBuilder.aTraining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INSTRUCTOR = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EXERCISE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final TrainingPeriod PERIOD =
        new TrainingPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static TrainingItem item() {
        return new TrainingItem(EXERCISE, 4, 10, new BigDecimal("40.00"), 60);
    }

    private static TrainingDraft draft(String name, List<TrainingItem> items) {
        return new TrainingDraft(name, "Hipertrofia", PERIOD, items);
    }

    @Nested
    class Create {

        @Test
        void givenValidDraft_whenCreating_thenActiveWithItemsAndEvent() {
            // When
            var training = Training.create(STUDENT, INSTRUCTOR, draft("Treino A", List.of(item())), CLOCK);

            // Then
            assertThat(training.status()).isEqualTo(TrainingStatus.ACTIVE);
            assertThat(training.studentId()).isEqualTo(STUDENT);
            assertThat(training.items()).hasSize(1);
            assertThat(training.createdAt()).isEqualTo(NOW);
            assertThat(training.pullDomainEvents()).singleElement().isInstanceOf(TrainingCreated.class);
        }

        @Test
        void givenDraftWithoutItems_whenCreating_thenThrows() {
            assertThatThrownBy(() -> Training.create(STUDENT, INSTRUCTOR, draft("Treino A", List.of()), CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void givenActiveTrainingWithinPeriod_whenCheckingActiveOnDate_thenTrue() {
            var training = Training.create(STUDENT, INSTRUCTOR, draft("Treino A", List.of(item())), CLOCK);
            assertThat(training.isActiveOn(LocalDate.of(2026, 7, 1))).isTrue();
            assertThat(training.isActiveOn(LocalDate.of(2026, 9, 1))).isFalse();
        }
    }

    @Nested
    class Ownership {

        @Test
        void givenTraining_whenCheckingOwnership_thenOnlyTheCreatingInstructorOwnsIt() {
            var training = aTraining().build(); // instructor = TrainingTestBuilder.INSTRUCTOR_ID
            assertThat(training.isOwnedBy(TrainingTestBuilder.INSTRUCTOR_ID)).isTrue();
            assertThat(training.isOwnedBy(UUID.randomUUID())).isFalse();
        }
    }

    @Nested
    class Update {

        @Test
        void givenActiveTraining_whenUpdatingName_thenAppliesAndRecordsChangedFields() {
            // Given
            var training = aTraining().build(); // name "Treino A"
            var newDraft = draft("Treino B", training.items());

            // When — keep status (null)
            var result = training.update(newDraft, null, CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(training.name()).isEqualTo("Treino B");
            assertThat(training.updatedAt()).isEqualTo(NOW);
            var events = training.pullDomainEvents();
            assertThat(events).singleElement().isInstanceOf(TrainingUpdated.class);
            assertThat(((TrainingUpdated) events.get(0)).changedFields()).contains("name");
        }

        @Test
        void givenActiveTraining_whenArchivingViaUpdate_thenStatusBecomesArchived() {
            // Given
            var training = aTraining().build();

            // When
            var result = training.update(draft("Treino A", training.items()), TrainingStatus.ARCHIVED, CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(training.status()).isEqualTo(TrainingStatus.ARCHIVED);
            assertThat(((TrainingUpdated) training.pullDomainEvents().get(0)).changedFields()).contains("status");
        }

        @Test
        void givenArchivedTraining_whenUpdating_thenFailsAndRaisesNoEvent() {
            // Given
            var training = aTraining().withStatus(TrainingStatus.ARCHIVED).build();

            // When
            var result = training.update(draft("Treino B", training.items()), null, CLOCK);

            // Then
            assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_ARCHIVED)).isTrue();
            assertThat(training.pullDomainEvents()).isEmpty();
        }
    }
}
