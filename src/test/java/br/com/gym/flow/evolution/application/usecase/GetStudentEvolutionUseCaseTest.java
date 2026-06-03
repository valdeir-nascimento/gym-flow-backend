package br.com.gym.flow.evolution.application.usecase;

import br.com.gym.flow.evolution.domain.Granularity;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionDirectory;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionItemView;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentEvolutionUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRAINING = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID BENCH = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    @Mock
    private WorkoutExecutionDirectory executions;

    private GetStudentEvolutionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStudentEvolutionUseCase(executions, CLOCK);
    }

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static GetStudentEvolutionQuery query(
        final UUID studentId, final Instant from, final Instant to, final UUID actorId, final String role) {
        return new GetStudentEvolutionQuery(studentId, Granularity.MONTHLY, from, to, actorId, role);
    }

    @Test
    void givenStudentRequestingAnotherStudentsEvolution_whenGetting_thenFailsForbidden() {
        // When
        var result = useCase.execute(query(UUID.randomUUID(), null, null, STUDENT, "STUDENT"));

        // Then — 403, history is never read
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EVOLUTION_NOT_OWNED)).isTrue();
        verifyNoInteractions(executions);
    }

    @Test
    void givenWindowLongerThan24Months_whenGetting_thenFailsPeriodTooLong() {
        // Given — from is 25 months before to
        Instant to = Instant.parse("2026-06-30T00:00:00Z");
        Instant from = Instant.parse("2024-05-30T00:00:00Z");

        // When
        var result = useCase.execute(query(STUDENT, from, to, STUDENT, "STUDENT"));

        // Then — 422, history is never read
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EVOLUTION_PERIOD_TOO_LONG)).isTrue();
        verifyNoInteractions(executions);
    }

    @Test
    void givenNoExecutions_whenGetting_thenSucceedsWithEmptyReport() {
        // Given
        when(executions.findByStudentInWindow(eq(STUDENT), any(), any())).thenReturn(List.of());

        // When
        var result = useCase.execute(query(STUDENT, null, null, STUDENT, "STUDENT"));

        // Then — 200 with empty series (absence of data is not an error)
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().frequency()).isEmpty();
        assertThat(result.getOrThrow().bodyweight()).isEmpty();
    }

    @Test
    void givenAdministrator_whenGettingAnotherStudentsEvolution_thenAllowed() {
        // Given — Administrator bypasses ownership
        when(executions.findByStudentInWindow(eq(STUDENT), any(), any())).thenReturn(List.of());

        // When
        var result = useCase.execute(query(STUDENT, null, null, UUID.randomUUID(), "ADMINISTRATOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().studentId()).isEqualTo(STUDENT);
    }

    @Test
    void givenOwnExecutions_whenGetting_thenComputesIndicators() {
        // Given — one bench session in June: 4×10×40 = 1600 volume
        var execution = new WorkoutExecutionView(UUID.randomUUID(), STUDENT, TRAINING,
            Instant.parse("2026-06-10T10:00:00Z"), Instant.parse("2026-06-10T11:00:00Z"), null,
            List.of(new WorkoutExecutionItemView(BENCH, 4, 10, new BigDecimal("40.00"), null)),
            Instant.parse("2026-06-10T11:00:00Z"));
        when(executions.findByStudentInWindow(eq(STUDENT), any(), any())).thenReturn(List.of(execution));

        // When
        var result = useCase.execute(query(STUDENT, null, null, STUDENT, "STUDENT"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().frequency()).singleElement()
            .satisfies(p -> assertThat(p.workouts()).isEqualTo(1));
        assertThat(result.getOrThrow().volume()).singleElement()
            .satisfies(p -> assertThat(p.totalVolume()).isEqualByComparingTo("1600.00"));
        assertThat(result.getOrThrow().oneRepMaxByExercise()).singleElement()
            .satisfies(s -> assertThat(s.exerciseId()).isEqualTo(BENCH));
        verify(executions).findByStudentInWindow(eq(STUDENT), any(), any());
    }
}
