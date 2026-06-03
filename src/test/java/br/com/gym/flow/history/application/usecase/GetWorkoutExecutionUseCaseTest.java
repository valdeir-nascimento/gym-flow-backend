package br.com.gym.flow.history.application.usecase;

import br.com.gym.flow.history.domain.ExecutedExercise;
import br.com.gym.flow.history.domain.WorkoutExecution;
import br.com.gym.flow.history.domain.WorkoutExecutionId;
import br.com.gym.flow.history.domain.WorkoutExecutionRepository;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWorkoutExecutionUseCaseTest {

    private static final WorkoutExecutionId EXECUTION_ID =
        WorkoutExecutionId.of(UUID.fromString("00000000-0000-0000-0000-0000000000a0"));
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRAINING = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID EXERCISE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final Instant START = Instant.parse("2026-06-01T11:00:00Z");

    @Mock
    private WorkoutExecutionRepository repository;

    private GetWorkoutExecutionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetWorkoutExecutionUseCase(repository);
    }

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static WorkoutExecution execution() {
        return WorkoutExecution.hydrate(
            EXECUTION_ID, STUDENT, TRAINING, START, START.plusSeconds(2700), "ok",
            List.of(new ExecutedExercise(EXERCISE, 4, 10, new BigDecimal("40.00"), null)),
            Instant.parse("2026-06-01T12:00:00Z"));
    }

    @Test
    void givenOwnerRequestingExistingExecution_whenGetById_thenReturnsView() {
        // Given
        when(repository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution()));

        // When
        var result = useCase.execute(new GetWorkoutExecutionQuery(EXECUTION_ID, STUDENT, "STUDENT"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().id()).isEqualTo(EXECUTION_ID.value());
        assertThat(result.getOrThrow().items()).hasSize(1);
    }

    @Test
    void givenUnknownExecution_whenGetById_thenFailsNotFound() {
        // Given
        when(repository.findById(EXECUTION_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new GetWorkoutExecutionQuery(EXECUTION_ID, STUDENT, "STUDENT"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_NOT_FOUND)).isTrue();
    }

    @Test
    void givenExecutionOfAnotherStudent_whenGetById_thenFailsForbidden() {
        // Given — execution belongs to STUDENT, another student is asking
        when(repository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution()));

        // When
        var result = useCase.execute(new GetWorkoutExecutionQuery(EXECUTION_ID, UUID.randomUUID(), "STUDENT"));

        // Then — 403, not 404 (the resource exists)
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_NOT_OWNED)).isTrue();
    }

    @Test
    void givenAdministrator_whenGetByIdOfAnotherStudent_thenAllowed() {
        // Given — Administrator bypasses ownership
        when(repository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution()));

        // When
        var result = useCase.execute(new GetWorkoutExecutionQuery(EXECUTION_ID, UUID.randomUUID(), "ADMINISTRATOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().studentId()).isEqualTo(STUDENT);
    }
}
