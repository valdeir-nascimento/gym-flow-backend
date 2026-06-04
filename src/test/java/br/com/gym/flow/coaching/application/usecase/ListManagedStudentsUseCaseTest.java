package br.com.gym.flow.coaching.application.usecase;

import br.com.gym.flow.coaching.domain.spi.ManagedStudentView;
import br.com.gym.flow.evolution.domain.spi.StudentActivitySummary;
import br.com.gym.flow.evolution.domain.spi.StudentProgressDirectory;
import br.com.gym.flow.users.domain.spi.TeacherStudentDirectory;
import br.com.gym.flow.users.domain.spi.UserDirectory;
import br.com.gym.flow.users.domain.spi.UserView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListManagedStudentsUseCaseTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);
    private static final UUID INSTRUCTOR = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BRUNO = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID CARLA = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

    @Mock
    private UserDirectory users;
    @Mock
    private TeacherStudentDirectory bonds;
    @Mock
    private StudentProgressDirectory progress;

    private ListManagedStudentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListManagedStudentsUseCase(users, bonds, progress);
    }

    private static UserView student(final UUID id, final String name) {
        return new UserView(id, name, name.toLowerCase() + "@mail.com", null, LocalDate.of(2000, 1, 1),
            "STUDENT", "ACTIVE", null, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void givenInstructorWithStudents_whenListing_thenOrdersByLastActivityThenName() {
        // Given — Bruno trained most recently; Ana and Carla never trained (alphabetical)
        when(bonds.activeStudentIdsOf(INSTRUCTOR)).thenReturn(List.of(ANA, BRUNO, CARLA));
        when(users.findByIds(List.of(ANA, BRUNO, CARLA)))
            .thenReturn(List.of(student(ANA, "Ana"), student(BRUNO, "Bruno"), student(CARLA, "Carla")));
        when(progress.summariesOf(anyList())).thenReturn(List.of(
            new StudentActivitySummary(BRUNO, Instant.parse("2026-06-20T10:00:00Z"), 12),
            new StudentActivitySummary(ANA, null, 0),
            new StudentActivitySummary(CARLA, null, 0)));

        // When
        var result = useCase.execute(new ListManagedStudentsQuery(INSTRUCTOR, "INSTRUCTOR", PAGEABLE));

        // Then — Bruno first (recent activity), then Ana/Carla alphabetical (no activity)
        assertThat(result.isSuccess()).isTrue();
        List<ManagedStudentView> content = result.getOrThrow().getContent();
        assertThat(content).extracting(ManagedStudentView::name).containsExactly("Bruno", "Ana", "Carla");
        assertThat(content.get(0).totalExecutions()).isEqualTo(12);
        assertThat(content.get(1).lastExecutionAt()).isNull();
    }

    @Test
    void givenInstructorWithoutStudents_whenListing_thenReturnsEmptyPage() {
        // Given
        when(bonds.activeStudentIdsOf(INSTRUCTOR)).thenReturn(List.of());

        // When
        var result = useCase.execute(new ListManagedStudentsQuery(INSTRUCTOR, "INSTRUCTOR", PAGEABLE));

        // Then — empty, and neither users nor progress are queried
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getTotalElements()).isZero();
        verifyNoInteractions(users, progress);
    }

    @Test
    void givenAdministrator_whenListing_thenUsesAllStudentsIgnoringBonds() {
        // Given — Administrator accompanies everyone
        when(users.findStudents()).thenReturn(List.of(student(ANA, "Ana")));
        when(progress.summariesOf(anyList()))
            .thenReturn(List.of(new StudentActivitySummary(ANA, null, 0)));

        // When
        var result = useCase.execute(new ListManagedStudentsQuery(UUID.randomUUID(), "ADMINISTRATOR", PAGEABLE));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getContent()).extracting(ManagedStudentView::name).containsExactly("Ana");
        verifyNoInteractions(bonds);
    }
}
