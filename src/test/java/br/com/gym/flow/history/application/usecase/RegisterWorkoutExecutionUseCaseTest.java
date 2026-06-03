package br.com.gym.flow.history.application.usecase;

import br.com.gym.flow.history.domain.ExecutedExercise;
import br.com.gym.flow.history.domain.WorkoutExecutionRepository;
import br.com.gym.flow.history.events.WorkoutExecutionRegistered;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.domain.spi.TrainingDigest;
import br.com.gym.flow.trainings.domain.spi.TrainingDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterWorkoutExecutionUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Instant START = NOW.minus(Duration.ofHours(1));
    private static final Instant END = NOW.minus(Duration.ofMinutes(30));
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRAINING = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID EXERCISE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    @Mock
    private WorkoutExecutionRepository executions;
    @Mock
    private TrainingDirectory trainings;
    @Mock
    private ApplicationEventPublisher events;

    private RegisterWorkoutExecutionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterWorkoutExecutionUseCase(executions, trainings, events, CLOCK);
    }

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static RegisterWorkoutExecutionCommand command(final Instant start, final Instant end, final UUID actorId) {
        ExecutedExercise item = new ExecutedExercise(EXERCISE, 4, 10, new BigDecimal("40.00"), "ok");
        return new RegisterWorkoutExecutionCommand(TRAINING, actorId, start, end, "notes", List.of(item));
    }

    private static TrainingDigest active() {
        return new TrainingDigest(TRAINING, STUDENT, "ACTIVE", null);
    }

    private static TrainingDigest archivedAt(final Instant inactivatedAt) {
        return new TrainingDigest(TRAINING, STUDENT, "ARCHIVED", inactivatedAt);
    }

    @Test
    void givenBlankItems_whenRegistering_thenFailsValidationBeforeLookup() {
        // Given — empty items (structural)
        var command = new RegisterWorkoutExecutionCommand(TRAINING, STUDENT, START, END, null, List.of());

        // When
        var result = useCase.execute(command);

        // Then — 400 and the training is never resolved
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
        verifyNoInteractions(trainings, executions);
    }

    @Test
    void givenUnknownTraining_whenRegistering_thenFailsNotFound() {
        // Given
        when(trainings.findById(TRAINING)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(command(START, END, STUDENT));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_TRAINING_NOT_FOUND)).isTrue();
        verify(executions, never()).save(any());
    }

    @Test
    void givenTrainingOfAnotherStudent_whenRegistering_thenFailsNotOwned() {
        // Given — training belongs to STUDENT, but another student is acting
        when(trainings.findById(TRAINING)).thenReturn(Optional.of(active()));

        // When
        var result = useCase.execute(command(START, END, UUID.randomUUID()));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_TRAINING_NOT_OWNED)).isTrue();
        verifyNoInteractions(executions);
    }

    @Test
    void givenArchivedTrainingStartedAfterInactivation_whenRegistering_thenFailsOutOfWindow() {
        // Given — inactivated before the session started
        when(trainings.findById(TRAINING)).thenReturn(Optional.of(archivedAt(START.minus(Duration.ofMinutes(1)))));

        // When
        var result = useCase.execute(command(START, END, STUDENT));

        // Then — 422
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_TRAINING_INACTIVE_OUT_OF_WINDOW)).isTrue();
        verifyNoInteractions(executions);
    }

    @Test
    void givenArchivedTrainingStartedWithinWindow_whenRegistering_thenSucceeds() {
        // Given — inactivated after the session started (tolerance window)
        when(trainings.findById(TRAINING)).thenReturn(Optional.of(archivedAt(NOW)));
        when(executions.existsByStudentAndTrainingAndStart(STUDENT, TRAINING, START)).thenReturn(false);
        when(executions.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command(START, END, STUDENT));

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(events).publishEvent(any(WorkoutExecutionRegistered.class));
    }

    @Test
    void givenDuplicateExecution_whenRegistering_thenFailsAlreadyRegistered() {
        // Given — idempotency key already taken
        when(trainings.findById(TRAINING)).thenReturn(Optional.of(active()));
        when(executions.existsByStudentAndTrainingAndStart(STUDENT, TRAINING, START)).thenReturn(true);

        // When
        var result = useCase.execute(command(START, END, STUDENT));

        // Then — 409
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_ALREADY_REGISTERED)).isTrue();
        verify(executions, never()).save(any());
    }

    @Test
    void givenFutureTimestamp_whenRegistering_thenFailsFutureDatetime() {
        // Given — active training but a future finish
        when(trainings.findById(TRAINING)).thenReturn(Optional.of(active()));
        when(executions.existsByStudentAndTrainingAndStart(eq(STUDENT), eq(TRAINING), any())).thenReturn(false);

        // When
        var result = useCase.execute(command(NOW.minus(Duration.ofMinutes(10)), NOW.plus(Duration.ofHours(1)), STUDENT));

        // Then — 400
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_FUTURE_DATETIME)).isTrue();
        verify(executions, never()).save(any());
    }

    @Test
    void givenEndBeforeStart_whenRegistering_thenFailsEndBeforeStart() {
        // Given
        when(trainings.findById(TRAINING)).thenReturn(Optional.of(active()));
        when(executions.existsByStudentAndTrainingAndStart(eq(STUDENT), eq(TRAINING), any())).thenReturn(false);

        // When — finish one hour before start
        var result = useCase.execute(command(NOW.minus(Duration.ofMinutes(30)), NOW.minus(Duration.ofHours(1)), STUDENT));

        // Then — 422
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_END_BEFORE_START)).isTrue();
        verify(executions, never()).save(any());
    }

    @Test
    void givenActiveTrainingAndValidData_whenRegistering_thenPersistsAndPublishesEvent() {
        // Given
        when(trainings.findById(TRAINING)).thenReturn(Optional.of(active()));
        when(executions.existsByStudentAndTrainingAndStart(STUDENT, TRAINING, START)).thenReturn(false);
        when(executions.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command(START, END, STUDENT));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().studentId()).isEqualTo(STUDENT);
        assertThat(result.getOrThrow().trainingId()).isEqualTo(TRAINING);
        verify(executions).save(any());
        verify(events).publishEvent(any(WorkoutExecutionRegistered.class));
    }
}
