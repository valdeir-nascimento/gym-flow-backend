package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.events.UserRoleChanged;
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
class ChangeUserRoleUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    private static final UserId ACTOR = UserId.of(UUID.fromString("00000000-0000-0000-0000-0000000000ad"));

    @Mock
    private UserRepository repository;
    @Mock
    private ApplicationEventPublisher events;

    private ChangeUserRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeUserRoleUseCase(repository, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenActorChangingOwnRole_whenChanging_thenFailsSelfManagement() {
        // When — actor equals the target user
        var result = useCase.execute(new ChangeUserRoleCommand(USER_ID, Role.INSTRUCTOR, USER_ID));

        // Then — 422, repository untouched
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_SELF_MANAGEMENT)).isTrue();
        verifyNoInteractions(repository, events);
    }

    @Test
    void givenUnknownUser_whenChanging_thenFailsNotFound() {
        // Given
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new ChangeUserRoleCommand(USER_ID, Role.INSTRUCTOR, ACTOR));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_FOUND)).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void givenLastActiveAdministrator_whenDemoting_thenFailsLastAdministrator() {
        // Given — the only active admin being demoted
        User lastAdmin = aUser().withId(USER_ID).withRole(Role.ADMINISTRATOR).withStatus(UserStatus.ACTIVE).build();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(lastAdmin));
        when(repository.countActiveAdministrators()).thenReturn(1L);

        // When
        var result = useCase.execute(new ChangeUserRoleCommand(USER_ID, Role.INSTRUCTOR, ACTOR));

        // Then — 409
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_LAST_ADMINISTRATOR)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenStudent_whenPromotingToInstructor_thenSucceedsAndPublishesRoleChanged() {
        // Given
        User student = aUser().withId(USER_ID).withRole(Role.STUDENT).withStatus(UserStatus.ACTIVE).build();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(student));
        when(repository.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(new ChangeUserRoleCommand(USER_ID, Role.INSTRUCTOR, ACTOR));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().role()).isEqualTo("INSTRUCTOR");
        verify(repository).save(student);
        verify(events).publishEvent(any(UserRoleChanged.class));
    }
}
