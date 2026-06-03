package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.spi.BondView;
import br.com.gym.flow.users.events.UserRegistered;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterStudentUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UserId INSTRUCTOR_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final LocalDate VALID_BIRTH = LocalDate.of(2000, 1, 1);

    @Mock
    private UserRepository repository;
    @Mock
    private AssignStudentToInstructorUseCase assignBond;
    @Mock
    private ApplicationEventPublisher events;

    private RegisterStudentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterStudentUseCase(repository, assignBond, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static RegisterStudentCommand validCommand(UserId createdBy, Role createdByRole) {
        return new RegisterStudentCommand(
            "Maria Silva", "maria@example.com", "+5511912345678", VALID_BIRTH, createdBy, createdByRole);
    }

    private static BondView sampleBond(UUID studentId) {
        return new BondView(UUID.randomUUID(), studentId, INSTRUCTOR_ID.value(),
            Instant.parse("2026-06-01T12:00:00Z"), null, INSTRUCTOR_ID.value(), true);
    }

    @Test
    void givenInvalidEmail_whenRegistering_thenFailsValidationAndTouchesNothing() {
        // Given
        var command = new RegisterStudentCommand(
            "Maria Silva", "not-an-email", "+5511912345678", VALID_BIRTH, null, null);

        // When
        var result = useCase.execute(command);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_EMAIL)).isTrue();
        verifyNoInteractions(repository, assignBond, events);
    }

    @Test
    void givenEmailAlreadyTaken_whenRegistering_thenFailsConflictAndPersistsNothing() {
        // Given
        when(repository.existsByEmail(any())).thenReturn(true);

        // When
        var result = useCase.execute(validCommand(null, null));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_EMAIL_TAKEN)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(assignBond, events);
    }

    @Test
    void givenNonInstructorCreator_whenRegistering_thenPersistsPublishesAndSkipsAutoBond() {
        // Given
        when(repository.existsByEmail(any())).thenReturn(false);
        when(repository.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(validCommand(null, null));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().role()).isEqualTo("STUDENT");
        verify(repository).save(any());
        verify(events).publishEvent(any(UserRegistered.class));
        verifyNoInteractions(assignBond); // no instructor creator -> no automatic bond
    }

    @Test
    void givenInstructorCreator_whenRegistering_thenAutoBondsTheNewStudent() {
        // Given
        when(repository.existsByEmail(any())).thenReturn(false);
        when(repository.save(any())).then(returnsFirstArg());
        when(assignBond.execute(any())).thenReturn(Result.success(sampleBond(UUID.randomUUID())));

        // When
        var result = useCase.execute(validCommand(INSTRUCTOR_ID, Role.INSTRUCTOR));

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(assignBond).execute(any(AssignStudentToInstructorCommand.class));
    }

    @Test
    void givenAutoBondFails_whenRegistering_thenPropagatesTheBondFailure() {
        // Given
        when(repository.existsByEmail(any())).thenReturn(false);
        when(repository.save(any())).then(returnsFirstArg());
        when(assignBond.execute(any())).thenReturn(Result.failWith(ErrorCode.BOND_INACTIVE_PARTICIPANT));

        // When
        var result = useCase.execute(validCommand(INSTRUCTOR_ID, Role.INSTRUCTOR));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.BOND_INACTIVE_PARTICIPANT)).isTrue();
    }
}
