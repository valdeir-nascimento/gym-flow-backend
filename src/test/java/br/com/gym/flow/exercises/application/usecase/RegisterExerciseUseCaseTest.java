package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import br.com.gym.flow.exercises.domain.MuscleGroup;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterExerciseUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Mock
    private ExerciseRepository repository;

    private RegisterExerciseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterExerciseUseCase(repository, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static RegisterExerciseCommand command(String name, String actorRole) {
        return new RegisterExerciseCommand(
            name, MuscleGroup.CHEST, "desc", "Barra", DifficultyLevel.INTERMEDIATE, null, null, ACTOR, actorRole);
    }

    @Test
    void givenStudentRole_whenRegistering_thenForbiddenAndTouchesNothing() {
        // When — a Student may not manage the catalog
        var result = useCase.execute(command("Supino Reto", "STUDENT"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.FORBIDDEN_ROLE)).isTrue();
        verifyNoInteractions(repository);
    }

    @Test
    void givenInstructorAndValidData_whenRegistering_thenPersistsActiveExercise() {
        // Given
        when(repository.existsByNameIgnoreCase("Supino Reto")).thenReturn(false);
        when(repository.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command("Supino Reto", "INSTRUCTOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().name()).isEqualTo("Supino Reto");
        assertThat(result.getOrThrow().status()).isEqualTo("ACTIVE");
        verify(repository).save(any());
    }

    @Test
    void givenDuplicateName_whenRegistering_thenFailsConflict() {
        // Given
        when(repository.existsByNameIgnoreCase("Supino Reto")).thenReturn(true);

        // When
        var result = useCase.execute(command("Supino Reto", "ADMINISTRATOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXERCISE_NAME_TAKEN)).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void givenBlankName_whenRegistering_thenValidationFailsBeforeHittingRepository() {
        // When — role is allowed, but the name is blank
        var result = useCase.execute(command("   ", "INSTRUCTOR"));

        // Then — validation short-circuits before the uniqueness check / persistence
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
        verifyNoInteractions(repository);
    }
}
