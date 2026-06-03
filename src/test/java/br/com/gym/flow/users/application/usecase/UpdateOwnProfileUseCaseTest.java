package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.events.UserProfileUpdated;
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
class UpdateOwnProfileUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));

    @Mock
    private UserRepository repository;
    @Mock
    private ApplicationEventPublisher events;

    private UpdateOwnProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateOwnProfileUseCase(repository, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static User activeUser() {
        return aUser().withId(USER_ID).withStatus(UserStatus.ACTIVE).withName("Maria Silva").build();
    }

    @Test
    void givenUnknownUser_whenUpdating_thenFailsNotFound() {
        // Given
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new UpdateOwnProfileCommand(USER_ID, "Maria Souza", null, null));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_FOUND)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenInactiveUser_whenUpdating_thenFailsUserInactive() {
        // Given — only ACTIVE users may edit their own profile
        User blocked = aUser().withId(USER_ID).withStatus(UserStatus.BLOCKED).build();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(blocked));

        // When
        var result = useCase.execute(new UpdateOwnProfileCommand(USER_ID, "Maria Souza", null, null));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_INACTIVE)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenValidName_whenUpdating_thenPersistsAndPublishesProfileUpdated() {
        // Given
        when(repository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(repository.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(new UpdateOwnProfileCommand(USER_ID, "Maria Souza", null, null));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().name()).isEqualTo("Maria Souza");
        verify(repository).save(any());
        verify(events).publishEvent(any(UserProfileUpdated.class));
    }

    @Test
    void givenInvalidPhone_whenUpdating_thenPropagatesValidationFailureAndPersistsNothing() {
        // Given
        when(repository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));

        // When
        var result = useCase.execute(new UpdateOwnProfileCommand(USER_ID, null, "not-a-phone", null));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_PHONE)).isTrue();
        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }
}
