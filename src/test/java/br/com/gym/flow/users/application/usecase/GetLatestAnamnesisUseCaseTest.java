package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisId;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisRepository;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisRevision;
import br.com.gym.flow.users.domain.anamnesis.Height;
import br.com.gym.flow.users.domain.anamnesis.Weight;
import br.com.gym.flow.users.domain.bond.BondId;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.bond.TeacherStudentBond;
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
class GetLatestAnamnesisUseCaseTest {

    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INSTRUCTOR = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private AnamnesisRepository anamneses;
    @Mock
    private BondRepository bonds;

    private GetLatestAnamnesisUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetLatestAnamnesisUseCase(anamneses, bonds);
    }

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static AnamnesisRevision revision() {
        return AnamnesisRevision.hydrate(AnamnesisId.newId(), STUDENT, 1,
            Weight.of(new BigDecimal("80.00")), Height.of(175), "Hipertrofia", "iniciante",
            List.of(), List.of(), List.of(), "obs", INSTRUCTOR, Instant.parse("2026-06-01T12:00:00Z"));
    }

    private static TeacherStudentBond activeBond() {
        return TeacherStudentBond.hydrate(BondId.newId(), UserId.of(STUDENT), UserId.of(INSTRUCTOR),
            Instant.parse("2026-01-01T00:00:00Z"), null, UserId.of(INSTRUCTOR));
    }

    @Test
    void givenStudentReadingOwn_whenGetLatest_thenReturnsView() {
        // Given — the student themselves
        when(anamneses.findLatestByStudent(STUDENT)).thenReturn(Optional.of(revision()));

        // When
        var result = useCase.execute(new GetLatestAnamnesisQuery(STUDENT, STUDENT, "STUDENT"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().studentId()).isEqualTo(STUDENT);
    }

    @Test
    void givenBondedInstructor_whenGetLatest_thenReturnsView() {
        // Given — the bonded instructor
        when(bonds.findActiveByStudent(UserId.of(STUDENT))).thenReturn(Optional.of(activeBond()));
        when(anamneses.findLatestByStudent(STUDENT)).thenReturn(Optional.of(revision()));

        // When
        var result = useCase.execute(new GetLatestAnamnesisQuery(STUDENT, INSTRUCTOR, "INSTRUCTOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void givenUnrelatedActor_whenGetLatest_thenFailsNotOwned() {
        // Given — neither the student, an admin, nor the bonded instructor
        when(bonds.findActiveByStudent(UserId.of(STUDENT))).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new GetLatestAnamnesisQuery(STUDENT, UUID.randomUUID(), "INSTRUCTOR"));

        // Then — 403
        assertThat(failureOf(result).hasAnyCode(ErrorCode.ANAMNESIS_NOT_OWNED)).isTrue();
    }

    @Test
    void givenNoRevisions_whenGetLatest_thenFailsNotFound() {
        // Given — student self, but no anamnesis yet
        when(anamneses.findLatestByStudent(STUDENT)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new GetLatestAnamnesisQuery(STUDENT, STUDENT, "STUDENT"));

        // Then — 404
        assertThat(failureOf(result).hasAnyCode(ErrorCode.ANAMNESIS_NOT_FOUND)).isTrue();
    }
}
