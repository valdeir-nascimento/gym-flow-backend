package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.bond.TeacherStudentBond;
import br.com.gym.flow.users.events.BondCreated;
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

import static br.com.gym.flow.users.domain.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignStudentToInstructorUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UserId STUDENT_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final UserId INSTRUCTOR_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final UserId ACTOR_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));

    @Mock
    private UserRepository users;
    @Mock
    private BondRepository bonds;
    @Mock
    private ApplicationEventPublisher events;

    private AssignStudentToInstructorUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AssignStudentToInstructorUseCase(users, bonds, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private User activeStudent() {
        return aUser().withId(STUDENT_ID).withRole(Role.STUDENT).withStatus(UserStatus.ACTIVE).build();
    }

    private User activeInstructor() {
        return aUser().withId(INSTRUCTOR_ID).withRole(Role.INSTRUCTOR).withStatus(UserStatus.ACTIVE).build();
    }

    private AssignStudentToInstructorCommand command() {
        return new AssignStudentToInstructorCommand(STUDENT_ID, INSTRUCTOR_ID, ACTOR_ID);
    }

    @Test
    void givenUnknownStudent_whenAssigning_thenFailsNotFound() {
        // Given
        when(users.findById(STUDENT_ID)).thenReturn(Optional.empty());
        when(users.findById(INSTRUCTOR_ID)).thenReturn(Optional.of(activeInstructor()));

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_FOUND)).isTrue();
        verify(bonds, never()).save(any());
    }

    @Test
    void givenStudentIdThatIsActuallyAnInstructor_whenAssigning_thenFailsForbiddenRole() {
        // Given
        User notAStudent = aUser().withId(STUDENT_ID).withRole(Role.INSTRUCTOR).withStatus(UserStatus.ACTIVE).build();
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(notAStudent));
        when(users.findById(INSTRUCTOR_ID)).thenReturn(Optional.of(activeInstructor()));

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.FORBIDDEN_ROLE)).isTrue();
        verify(bonds, never()).save(any());
    }

    @Test
    void givenInactiveInstructor_whenAssigning_thenFailsInactiveParticipant() {
        // Given
        User inactiveInstructor = aUser().withId(INSTRUCTOR_ID).withRole(Role.INSTRUCTOR).withStatus(UserStatus.INACTIVE).build();
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(activeStudent()));
        when(users.findById(INSTRUCTOR_ID)).thenReturn(Optional.of(inactiveInstructor));

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.BOND_INACTIVE_PARTICIPANT)).isTrue();
        verify(bonds, never()).save(any());
    }

    @Test
    void givenStudentAlreadyBonded_whenAssigning_thenFailsConflict() {
        // Given — the student already has an active bond
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(activeStudent()));
        when(users.findById(INSTRUCTOR_ID)).thenReturn(Optional.of(activeInstructor()));
        TeacherStudentBond existing = TeacherStudentBond.open(STUDENT_ID, INSTRUCTOR_ID, ACTOR_ID, CLOCK).getOrThrow();
        when(bonds.findActiveByStudent(STUDENT_ID)).thenReturn(Optional.of(existing));

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.BOND_STUDENT_HAS_ACTIVE_INSTRUCTOR)).isTrue();
        verify(bonds, never()).save(any());
    }

    @Test
    void givenEligibleParticipants_whenAssigning_thenOpensBondPersistsAndPublishes() {
        // Given
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(activeStudent()));
        when(users.findById(INSTRUCTOR_ID)).thenReturn(Optional.of(activeInstructor()));
        when(bonds.findActiveByStudent(STUDENT_ID)).thenReturn(Optional.empty());
        when(bonds.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().studentId()).isEqualTo(STUDENT_ID.value());
        assertThat(result.getOrThrow().active()).isTrue();
        verify(bonds).save(any(TeacherStudentBond.class));
        verify(events).publishEvent(any(BondCreated.class));
    }
}
