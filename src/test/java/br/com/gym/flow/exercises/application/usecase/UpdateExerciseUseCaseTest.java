package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.ExerciseId;
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
import java.util.Optional;
import java.util.UUID;

import static br.com.gym.flow.exercises.domain.ExerciseTestBuilder.anExercise;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateExerciseUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final ExerciseId EXERCISE_ID = ExerciseId.of(UUID.fromString("00000000-0000-0000-0000-000000000010"));

    @Mock
    private ExerciseRepository repository;

    private UpdateExerciseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateExerciseUseCase(repository, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static UpdateExerciseCommand command(String name, String actorRole) {
        return new UpdateExerciseCommand(
            EXERCISE_ID, name, MuscleGroup.CHEST, "desc", "Barra", DifficultyLevel.INTERMEDIATE, null, null, actorRole);
    }

    @Test
    void givenStudentRole_whenUpdating_thenForbidden() {
        // When
        var result = useCase.execute(command("Novo Nome", "STUDENT"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.FORBIDDEN_ROLE)).isTrue();
        verifyNoInteractions(repository);
    }

    @Test
    void givenUnknownExercise_whenUpdating_thenFailsNotFound() {
        // Given
        when(repository.findById(EXERCISE_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(command("Novo Nome", "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXERCISE_NOT_FOUND)).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void givenRenameToExistingName_whenUpdating_thenFailsConflict() {
        // Given — exercise currently named "Supino Reto", renamed to a name already taken
        when(repository.findById(EXERCISE_ID))
            .thenReturn(Optional.of(anExercise().withId(EXERCISE_ID).withName("Supino Reto").build()));
        when(repository.existsByNameIgnoreCase("Agachamento")).thenReturn(true);

        // When
        var result = useCase.execute(command("Agachamento", "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXERCISE_NAME_TAKEN)).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void givenSameName_whenUpdating_thenSkipsUniquenessCheckAndSucceeds() {
        // Given — the name is unchanged (case-insensitive), so it must not be flagged as duplicate
        when(repository.findById(EXERCISE_ID))
            .thenReturn(Optional.of(anExercise().withId(EXERCISE_ID).withName("Supino Reto").build()));
        when(repository.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command("supino reto", "INSTRUCTOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(repository, never()).existsByNameIgnoreCase(any());
        verify(repository).save(any());
    }

    @Test
    void givenValidRename_whenUpdating_thenAppliesAndPersists() {
        // Given
        when(repository.findById(EXERCISE_ID))
            .thenReturn(Optional.of(anExercise().withId(EXERCISE_ID).withName("Supino Reto").build()));
        when(repository.existsByNameIgnoreCase("Crucifixo")).thenReturn(false);
        when(repository.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command("Crucifixo", "INSTRUCTOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().name()).isEqualTo("Crucifixo");
        verify(repository).save(any());
    }
}
