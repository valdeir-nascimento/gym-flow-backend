package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.domain.ExerciseId;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import br.com.gym.flow.exercises.domain.ExerciseStatus;
import br.com.gym.flow.exercises.events.ExerciseDeactivated;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
class DeactivateExerciseUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final ExerciseId EXERCISE_ID = ExerciseId.of(UUID.fromString("00000000-0000-0000-0000-000000000010"));

    @Mock
    private ExerciseRepository repository;
    @Mock
    private ApplicationEventPublisher events;

    private DeactivateExerciseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeactivateExerciseUseCase(repository, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenInstructorRole_whenDeactivating_thenForbidden() {
        // When — only Administrator may deactivate (RF-011)
        var result = useCase.execute(new DeactivateExerciseCommand(EXERCISE_ID, "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.FORBIDDEN_ROLE)).isTrue();
        verifyNoInteractions(repository, events);
    }

    @Test
    void givenAdminAndUnknownExercise_whenDeactivating_thenFailsNotFound() {
        // Given
        when(repository.findById(EXERCISE_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new DeactivateExerciseCommand(EXERCISE_ID, "ADMINISTRATOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXERCISE_NOT_FOUND)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenAdminAndAlreadyInactive_whenDeactivating_thenPropagatesFailure() {
        // Given
        when(repository.findById(EXERCISE_ID))
            .thenReturn(Optional.of(anExercise().withId(EXERCISE_ID).withStatus(ExerciseStatus.INACTIVE).build()));

        // When
        var result = useCase.execute(new DeactivateExerciseCommand(EXERCISE_ID, "ADMINISTRATOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXERCISE_ALREADY_INACTIVE)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenAdminAndActiveExercise_whenDeactivating_thenPersistsAndPublishesEvent() {
        // Given
        when(repository.findById(EXERCISE_ID))
            .thenReturn(Optional.of(anExercise().withId(EXERCISE_ID).withStatus(ExerciseStatus.ACTIVE).build()));
        when(repository.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(new DeactivateExerciseCommand(EXERCISE_ID, "ADMINISTRATOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().status()).isEqualTo("INACTIVE");
        verify(repository).save(any());
        verify(events).publishEvent(any(ExerciseDeactivated.class));
    }
}
