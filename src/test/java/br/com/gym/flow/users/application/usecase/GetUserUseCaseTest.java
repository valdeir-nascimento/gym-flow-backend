package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static br.com.gym.flow.users.domain.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserUseCaseTest {

    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-0000000000d1"));

    @Mock
    private UserRepository repository;

    private GetUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetUserUseCase(repository);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenExistingUser_whenGetting_thenReturnsItsView() {
        // Given
        when(repository.findById(USER_ID)).thenReturn(Optional.of(aUser().withId(USER_ID).build()));

        // When
        var result = useCase.execute(new GetUserQuery(USER_ID));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().id()).isEqualTo(USER_ID.value());
    }

    @Test
    void givenUnknownUser_whenGetting_thenFailsNotFound() {
        // Given
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new GetUserQuery(USER_ID));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_FOUND)).isTrue();
    }
}
