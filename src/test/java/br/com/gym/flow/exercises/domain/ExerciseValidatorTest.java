package br.com.gym.flow.exercises.domain;

import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.NotificationError;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseValidatorTest {

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenValidFields_whenValidating_thenTrimsNameAndKeepsUrls() {
        // When
        var result = ExerciseValidator.validate(
            "  Supino Reto  ", MuscleGroup.CHEST, " peito ", " Barra ", DifficultyLevel.INTERMEDIATE,
            "https://cdn.example.com/v.mp4", "https://cdn.example.com/i.png");

        // Then
        assertThat(result.isSuccess()).isTrue();
        var details = result.getOrThrow();
        assertThat(details.name()).isEqualTo("Supino Reto");          // trimmed
        assertThat(details.description()).isEqualTo("peito");
        assertThat(details.equipment()).isEqualTo("Barra");
        assertThat(details.videoUrl()).isEqualTo("https://cdn.example.com/v.mp4");
    }

    @Test
    void givenBlankOptionalFields_whenValidating_thenNormalizesThemToNull() {
        // When — blank description/equipment and no URLs
        var result = ExerciseValidator.validate(
            "Agachamento", MuscleGroup.LEGS, "   ", "", DifficultyLevel.BEGINNER, null, null);

        // Then
        assertThat(result.isSuccess()).isTrue();
        var details = result.getOrThrow();
        assertThat(details.description()).isNull();
        assertThat(details.equipment()).isNull();
        assertThat(details.videoUrl()).isNull();
    }

    @Test
    void givenBlankName_whenValidating_thenFailsOnNameField() {
        // When
        var result = ExerciseValidator.validate(
            "  ", MuscleGroup.CHEST, "x", "Barra", DifficultyLevel.INTERMEDIATE, null, null);

        // Then
        assertThat(failureOf(result).errors()).extracting(NotificationError::field).contains("name");
    }

    @Test
    void givenMissingEnums_whenValidating_thenFailsOnBothEnumFields() {
        // When
        var result = ExerciseValidator.validate("Supino", null, "x", "Barra", null, null, null);

        // Then
        assertThat(failureOf(result).errors())
            .extracting(NotificationError::field)
            .contains("muscleGroup", "difficultyLevel");
    }

    @Test
    void givenMalformedUrl_whenValidating_thenFailsOnUrlField() {
        // When
        var result = ExerciseValidator.validate(
            "Supino", MuscleGroup.CHEST, "x", "Barra", DifficultyLevel.INTERMEDIATE, "not-a-url", null);

        // Then
        assertThat(failureOf(result).errors()).extracting(NotificationError::field).contains("videoUrl");
    }

    @Test
    void givenSeveralProblems_whenValidating_thenAccumulatesAllOfThem() {
        // When — blank name + missing group + bad image url, all at once
        var result = ExerciseValidator.validate(
            "", null, "x", "Barra", DifficultyLevel.INTERMEDIATE, null, "ftp://nope");

        // Then
        assertThat(failureOf(result).errors())
            .extracting(NotificationError::field)
            .contains("name", "muscleGroup", "imageUrl");
    }
}
