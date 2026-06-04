package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.domain.bond.BondId;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.bond.TeacherStudentBond;
import br.com.gym.flow.users.events.BondCreated;
import br.com.gym.flow.users.events.BondEnded;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferBondUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UserId STUDENT_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final UserId OLD_INSTRUCTOR_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final UserId NEW_INSTRUCTOR_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final UserId ACTOR_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));

    @Mock
    private UserRepository users;
    @Mock
    private BondRepository bonds;
    @Mock
    private ApplicationEventPublisher events;

    private TransferBondUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TransferBondUseCase(users, bonds, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private User activeStudent() {
        return aUser().withId(STUDENT_ID).withRole(Role.STUDENT).withStatus(UserStatus.ACTIVE).build();
    }

    private User activeNewInstructor() {
        return aUser().withId(NEW_INSTRUCTOR_ID).withRole(Role.INSTRUCTOR).withStatus(UserStatus.ACTIVE).build();
    }

    private TransferBondCommand command() {
        return new TransferBondCommand(STUDENT_ID, NEW_INSTRUCTOR_ID, ACTOR_ID);
    }

    @Test
    void givenUnknownNewInstructor_whenTransferring_thenFailsNotFound() {
        // Given
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(activeStudent()));
        when(users.findById(NEW_INSTRUCTOR_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_FOUND)).isTrue();
        verify(bonds, never()).save(any());
    }

    @Test
    void givenInactiveNewInstructor_whenTransferring_thenFailsInactiveParticipant() {
        // Given
        User inactive = aUser().withId(NEW_INSTRUCTOR_ID).withRole(Role.INSTRUCTOR).withStatus(UserStatus.INACTIVE).build();
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(activeStudent()));
        when(users.findById(NEW_INSTRUCTOR_ID)).thenReturn(Optional.of(inactive));

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.BOND_INACTIVE_PARTICIPANT)).isTrue();
        verify(bonds, never()).save(any());
    }

    @Test
    void givenInactiveStudent_whenTransferring_thenFailsInactiveParticipant() {
        // Given — the student being transferred is inactive
        User inactiveStudent = aUser().withId(STUDENT_ID).withRole(Role.STUDENT).withStatus(UserStatus.INACTIVE).build();
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(inactiveStudent));
        when(users.findById(NEW_INSTRUCTOR_ID)).thenReturn(Optional.of(activeNewInstructor()));

        // When
        var result = useCase.execute(command());

        // Then — 422, nothing persisted
        assertThat(failureOf(result).hasAnyCode(ErrorCode.BOND_INACTIVE_PARTICIPANT)).isTrue();
        verify(bonds, never()).save(any());
    }

    @Test
    void givenExistingActiveBond_whenTransferring_thenClosesOldOpensNewAndPublishesBoth() {
        // Given — a previously-persisted active bond, as loaded from the repo: hydrated, no pending events
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(activeStudent()));
        when(users.findById(NEW_INSTRUCTOR_ID)).thenReturn(Optional.of(activeNewInstructor()));
        TeacherStudentBond current = TeacherStudentBond.hydrate(
            BondId.newId(), STUDENT_ID, OLD_INSTRUCTOR_ID, Instant.parse("2026-01-01T00:00:00Z"), null, ACTOR_ID);
        when(bonds.findActiveByStudent(STUDENT_ID)).thenReturn(Optional.of(current));
        when(bonds.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(bonds, times(2)).save(any(TeacherStudentBond.class)); // old (closed) + new (opened)
        verify(events).publishEvent(any(BondEnded.class));
        verify(events).publishEvent(any(BondCreated.class));
    }

    @Test
    void givenNoExistingBond_whenTransferring_thenOpensNewAndPublishesCreatedOnly() {
        // Given
        when(users.findById(STUDENT_ID)).thenReturn(Optional.of(activeStudent()));
        when(users.findById(NEW_INSTRUCTOR_ID)).thenReturn(Optional.of(activeNewInstructor()));
        when(bonds.findActiveByStudent(STUDENT_ID)).thenReturn(Optional.empty());
        when(bonds.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(bonds, times(1)).save(any(TeacherStudentBond.class));
        verify(events).publishEvent(any(BondCreated.class));
        verify(events, never()).publishEvent(any(BondEnded.class));
    }
}
