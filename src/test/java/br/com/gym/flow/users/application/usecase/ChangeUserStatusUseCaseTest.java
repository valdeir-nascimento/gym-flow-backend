package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.events.UserActivated;
import br.com.gym.flow.users.events.UserDeactivated;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeUserStatusUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));

    @Mock
    private UserRepository repository;
    @Mock
    private ApplicationEventPublisher events;

    private ChangeUserStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeUserStatusUseCase(repository, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenUnknownUser_whenChangingStatus_thenFailsNotFoundAndPersistsNothing() {
        // Given
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new ChangeUserStatusCommand(USER_ID, UserStatus.ACTIVE));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_FOUND)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenLastActiveAdministrator_whenDeactivating_thenBlocksItAndPersistsNothing() {
        // Given — the only active admin
        User lastAdmin = aUser().withId(USER_ID).withRole(Role.ADMINISTRATOR).withStatus(UserStatus.ACTIVE).build();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(lastAdmin));
        when(repository.countActiveAdministrators()).thenReturn(1L);

        // When
        var result = useCase.execute(new ChangeUserStatusCommand(USER_ID, UserStatus.INACTIVE));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_USER_STATUS_TRANSITION)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenAdministratorWithOtherAdmins_whenDeactivating_thenSucceedsAndPublishesEvent() {
        // Given — another active admin keeps the system safe
        User admin = aUser().withId(USER_ID).withRole(Role.ADMINISTRATOR).withStatus(UserStatus.ACTIVE).build();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(admin));
        when(repository.countActiveAdministrators()).thenReturn(2L);
        when(repository.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(new ChangeUserStatusCommand(USER_ID, UserStatus.INACTIVE));

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(repository).save(admin);
        verify(events).publishEvent(any(UserDeactivated.class));
    }

    @Test
    void givenForbiddenTransition_whenChangingStatus_thenPropagatesDomainFailure() {
        // Given — PENDING_FIRST_ACCESS -> BLOCKED is rejected by the User state machine
        User pending = aUser().withId(USER_ID).withRole(Role.STUDENT).withStatus(UserStatus.PENDING_FIRST_ACCESS).build();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(pending));

        // When
        var result = useCase.execute(new ChangeUserStatusCommand(USER_ID, UserStatus.BLOCKED));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_USER_STATUS_TRANSITION)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenPendingUser_whenActivating_thenSucceedsAndReturnsActiveView() {
        // Given
        User pending = aUser().withId(USER_ID).withRole(Role.STUDENT).withStatus(UserStatus.PENDING_FIRST_ACCESS).build();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(pending));
        when(repository.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(new ChangeUserStatusCommand(USER_ID, UserStatus.ACTIVE));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().status()).isEqualTo("ACTIVE");
        verify(events).publishEvent(any(UserActivated.class));
    }
}
