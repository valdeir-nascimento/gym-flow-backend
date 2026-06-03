package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.bond.BondId;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.bond.TeacherStudentBond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListBondsUseCaseTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);
    private static final UserId STUDENT_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final UserId INSTRUCTOR_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    @Mock
    private BondRepository bonds;

    private ListBondsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListBondsUseCase(bonds);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static Page<TeacherStudentBond> oneBond() {
        TeacherStudentBond bond = TeacherStudentBond.hydrate(
            BondId.of(UUID.randomUUID()), STUDENT_ID, INSTRUCTOR_ID,
            Instant.parse("2026-01-01T00:00:00Z"), null, INSTRUCTOR_ID);
        return new PageImpl<>(List.of(bond), PAGEABLE, 1);
    }

    @Test
    void givenInstructorId_whenListing_thenQueriesByInstructor() {
        // Given
        when(bonds.findByInstructor(eq(INSTRUCTOR_ID), eq(PAGEABLE))).thenReturn(oneBond());

        // When
        var result = useCase.execute(new ListBondsQuery(INSTRUCTOR_ID, null, PAGEABLE));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getTotalElements()).isEqualTo(1);
    }

    @Test
    void givenStudentId_whenListing_thenQueriesByStudent() {
        // Given
        when(bonds.findByStudent(eq(STUDENT_ID), eq(PAGEABLE))).thenReturn(oneBond());

        // When
        var result = useCase.execute(new ListBondsQuery(null, STUDENT_ID, PAGEABLE));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getContent()).singleElement()
            .satisfies(view -> assertThat(view.studentId()).isEqualTo(STUDENT_ID.value()));
    }

    @Test
    void givenNeitherFilter_whenListing_thenFailsInvalidInput() {
        // When
        var result = useCase.execute(new ListBondsQuery(null, null, PAGEABLE));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
        verifyNoInteractions(bonds);
    }
}
