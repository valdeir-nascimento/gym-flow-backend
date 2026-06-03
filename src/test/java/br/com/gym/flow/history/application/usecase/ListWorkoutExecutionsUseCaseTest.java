package br.com.gym.flow.history.application.usecase;

import br.com.gym.flow.history.domain.ExecutedExercise;
import br.com.gym.flow.history.domain.WorkoutExecution;
import br.com.gym.flow.history.domain.WorkoutExecutionFilter;
import br.com.gym.flow.history.domain.WorkoutExecutionId;
import br.com.gym.flow.history.domain.WorkoutExecutionRepository;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListWorkoutExecutionsUseCaseTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRAINING = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID EXERCISE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final Instant START = Instant.parse("2026-06-01T11:00:00Z");

    @Mock
    private WorkoutExecutionRepository repository;

    private ListWorkoutExecutionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListWorkoutExecutionsUseCase(repository);
    }

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static WorkoutExecution execution() {
        return WorkoutExecution.hydrate(
            WorkoutExecutionId.of(UUID.fromString("00000000-0000-0000-0000-0000000000a0")),
            STUDENT, TRAINING, START, START.plusSeconds(2700), "ok",
            List.of(new ExecutedExercise(EXERCISE, 4, 10, new BigDecimal("40.00"), null)),
            Instant.parse("2026-06-01T12:00:00Z"));
    }

    private static ListWorkoutExecutionsQuery query(final UUID studentId, final UUID actorId, final String role) {
        return new ListWorkoutExecutionsQuery(studentId, null, null, null, null, actorId, role, PAGEABLE);
    }

    @Test
    void givenStudentRequestingOwnHistory_whenListing_thenReturnsMappedPage() {
        // Given
        Page<WorkoutExecution> page = new PageImpl<>(List.of(execution()), PAGEABLE, 1);
        when(repository.search(any(WorkoutExecutionFilter.class), eq(PAGEABLE))).thenReturn(page);

        // When
        var result = useCase.execute(query(STUDENT, STUDENT, "STUDENT"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getTotalElements()).isEqualTo(1);
        assertThat(result.getOrThrow().getContent()).singleElement()
            .satisfies(view -> assertThat(view.studentId()).isEqualTo(STUDENT));
    }

    @Test
    void givenStudentRequestingAnotherStudentsHistory_whenListing_thenFailsForbidden() {
        // Given — actor differs from the requested student
        // When
        var result = useCase.execute(query(UUID.randomUUID(), STUDENT, "STUDENT"));

        // Then — ownership policy (403); the repository is never touched
        assertThat(failureOf(result).hasAnyCode(ErrorCode.EXECUTION_NOT_OWNED)).isTrue();
        verifyNoInteractions(repository);
    }

    @Test
    void givenAdministrator_whenListingAnyStudentsHistory_thenAllowed() {
        // Given — Administrator bypasses ownership
        UUID someStudent = UUID.randomUUID();
        when(repository.search(any(WorkoutExecutionFilter.class), any())).thenReturn(Page.empty());

        // When
        var result = useCase.execute(query(someStudent, UUID.randomUUID(), "ADMINISTRATOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getTotalElements()).isZero();
    }

    @Test
    void givenFilters_whenListing_thenForwardsThemAsAFilter() {
        // Given
        when(repository.search(any(WorkoutExecutionFilter.class), any())).thenReturn(Page.empty());
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-30T23:59:59Z");

        // When
        useCase.execute(new ListWorkoutExecutionsQuery(
            STUDENT, TRAINING, EXERCISE, from, to, STUDENT, "STUDENT", PAGEABLE));

        // Then — query fields are forwarded verbatim into the domain filter
        var captor = ArgumentCaptor.forClass(WorkoutExecutionFilter.class);
        verify(repository).search(captor.capture(), eq(PAGEABLE));
        assertThat(captor.getValue().studentId()).isEqualTo(STUDENT);
        assertThat(captor.getValue().trainingId()).isEqualTo(TRAINING);
        assertThat(captor.getValue().exerciseId()).isEqualTo(EXERCISE);
        assertThat(captor.getValue().startedFrom()).isEqualTo(from);
        assertThat(captor.getValue().startedTo()).isEqualTo(to);
    }
}
