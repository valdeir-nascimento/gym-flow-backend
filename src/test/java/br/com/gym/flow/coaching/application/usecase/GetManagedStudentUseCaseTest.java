package br.com.gym.flow.coaching.application.usecase;

import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
import br.com.gym.flow.evolution.domain.spi.StudentProgressDirectory;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.spi.TeacherStudentDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetManagedStudentUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INSTRUCTOR = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private TeacherStudentDirectory bonds;
    @Mock
    private StudentProgressDirectory progress;

    private GetManagedStudentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetManagedStudentUseCase(bonds, progress, CLOCK);
    }

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static EvolutionReport emptyReport() {
        return new EvolutionReport(STUDENT, "WEEKLY", Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-06-30T00:00:00Z"), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void givenBondedStudent_whenGetting_thenReturnsReport() {
        // Given
        when(bonds.hasActiveBond(STUDENT, INSTRUCTOR)).thenReturn(true);
        when(progress.reportOf(eq(STUDENT), any(), any())).thenReturn(emptyReport());

        // When
        var result = useCase.execute(new GetManagedStudentQuery(STUDENT, INSTRUCTOR, "INSTRUCTOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().studentId()).isEqualTo(STUDENT);
    }

    @Test
    void givenUnbondedStudent_whenGetting_thenFailsNotManaged() {
        // Given — no bond and not an administrator
        when(bonds.hasActiveBond(STUDENT, INSTRUCTOR)).thenReturn(false);

        // When
        var result = useCase.execute(new GetManagedStudentQuery(STUDENT, INSTRUCTOR, "INSTRUCTOR"));

        // Then — 403, progress is never read
        assertThat(failureOf(result).hasAnyCode(ErrorCode.STUDENT_NOT_MANAGED)).isTrue();
        verifyNoInteractions(progress);
    }

    @Test
    void givenAdministrator_whenGettingUnbondedStudent_thenAllowed() {
        // Given — Administrator bypasses the bond check
        when(progress.reportOf(eq(STUDENT), any(), any())).thenReturn(emptyReport());

        // When
        var result = useCase.execute(new GetManagedStudentQuery(STUDENT, UUID.randomUUID(), "ADMINISTRATOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        verifyNoInteractions(bonds);
    }
}
