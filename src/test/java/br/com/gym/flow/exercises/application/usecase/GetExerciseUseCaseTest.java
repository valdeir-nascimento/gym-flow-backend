package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.domain.ExerciseId;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static br.com.gym.flow.exercises.domain.ExerciseTestBuilder.anExercise;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetExerciseUseCaseTest {

    private static final ExerciseId EXERCISE_ID = ExerciseId.of(UUID.fromString("00000000-0000-0000-0000-000000000010"));

    @Mock
    private ExerciseRepository repository;

    private GetExerciseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetExerciseUseCase(repository);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenExistingExercise_whenGetting_thenReturnsItsView() {
        // Given
        when(repository.findById(EXERCISE_ID)).thenReturn(Optional.of(anExercise().withId(EXERCISE_ID).build()));

        // When
        var result = useCase.execute(new GetExerciseQuery(EXERCISE_ID));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().id()).isEqualTo(EXERCISE_ID.value());
    }

    @Test
    void givenUnknownExercise_whenGetting_thenFailsNotFound() {
        // Given
        when(repository.findById(EXERCISE_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new GetExerciseQuery(EXERCISE_ID));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXERCISE_NOT_FOUND)).isTrue();
    }
}
