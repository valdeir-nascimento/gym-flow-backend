package br.com.gym.flow.exercises.domain;

import br.com.gym.flow.exercises.events.ExerciseDeactivated;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static br.com.gym.flow.exercises.domain.ExerciseTestBuilder.anExercise;
import static org.assertj.core.api.Assertions.assertThat;

class ExerciseTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID CREATED_BY = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final ExerciseDetails DETAILS = new ExerciseDetails("Supino Reto", MuscleGroup.CHEST, "desc", "Barra", DifficultyLevel.INTERMEDIATE, null, null);

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Nested
    class Create {

        @Test
        void givenDetails_whenCreating_thenStartsActiveWithNoEvents() {
            // When
            var exercise = Exercise.create(DETAILS, CREATED_BY, CLOCK);

            // Then
            assertThat(exercise.status()).isEqualTo(ExerciseStatus.ACTIVE);
            assertThat(exercise.name()).isEqualTo("Supino Reto");
            assertThat(exercise.muscleGroup()).isEqualTo(MuscleGroup.CHEST);
            assertThat(exercise.createdBy()).isEqualTo(CREATED_BY);
            assertThat(exercise.createdAt()).isEqualTo(NOW);
            assertThat(exercise.updatedAt()).isEqualTo(NOW);
            assertThat(exercise.pullDomainEvents()).isEmpty();
        }
    }

    @Nested
    class Update {

        @Test
        void givenNewDetails_whenUpdating_thenAppliesThemAndStampsUpdatedAt() {
            // Given
            var exercise = anExercise().withName("Supino Reto").build();
            var newDetails = new ExerciseDetails(
                "Supino Inclinado",
                MuscleGroup.SHOULDERS,
                "nova",
                "Halteres",
                DifficultyLevel.ADVANCED,
                null,
                null
            );

            // When
            exercise.update(newDetails, CLOCK);

            // Then
            assertThat(exercise.name()).isEqualTo("Supino Inclinado");
            assertThat(exercise.muscleGroup()).isEqualTo(MuscleGroup.SHOULDERS);
            assertThat(exercise.difficultyLevel()).isEqualTo(DifficultyLevel.ADVANCED);
            assertThat(exercise.updatedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    class Deactivate {

        @Test
        void givenActiveExercise_whenDeactivating_thenBecomesInactiveWithEvent() {
            // Given
            var exercise = anExercise().withStatus(ExerciseStatus.ACTIVE).build();

            // When
            var result = exercise.deactivate(CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(exercise.status()).isEqualTo(ExerciseStatus.INACTIVE);
            assertThat(exercise.updatedAt()).isEqualTo(NOW);
            assertThat(exercise.pullDomainEvents()).singleElement().isInstanceOf(ExerciseDeactivated.class);
        }

        @Test
        void givenAlreadyInactiveExercise_whenDeactivating_thenFailsAndRaisesNoEvent() {
            // Given
            var exercise = anExercise().withStatus(ExerciseStatus.INACTIVE).build();

            // When
            var result = exercise.deactivate(CLOCK);

            // Then
            assertThat(failureOf(result).hasAnyCode(ErrorCode.EXERCISE_ALREADY_INACTIVE)).isTrue();
            assertThat(exercise.pullDomainEvents()).isEmpty();
        }
    }
}
