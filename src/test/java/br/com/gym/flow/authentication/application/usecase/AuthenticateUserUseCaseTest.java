package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.authentication.application.service.EmailPasswordAuthenticator;
import br.com.gym.flow.authentication.application.service.TokenIssuer;
import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final String EMAIL = "maria@example.com";
    private static final String PASSWORD = "Senha@1234";
    private static final String IP = "203.0.113.7";

    @Mock
    private EmailPasswordAuthenticator authenticator;
    @Mock
    private TokenIssuer tokenIssuer;

    private AuthenticateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateUserUseCase(authenticator, tokenIssuer);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static UserCredentials activeCredentials() {
        return UserCredentials.hydrate(USER_ID, EMAIL, "enc:Senha@1234", Instant.parse("2026-01-01T00:00:00Z"), "STUDENT", UserCredentialsStatus.ACTIVE);
    }

    private static TokenPairView tokenPair() {
        return new TokenPairView("access-jwt", "raw-refresh", Instant.parse("2026-06-01T12:15:00Z"), Instant.parse("2026-06-08T12:00:00Z"), USER_ID, "STUDENT");
    }

    @Test
    void givenValidCredentials_whenAuthenticating_thenIssuesTokenPair() {
        // Given
        when(authenticator.authenticate(EMAIL, PASSWORD, IP)).thenReturn(Result.success(activeCredentials()));
        when(tokenIssuer.issue(USER_ID, "STUDENT")).thenReturn(tokenPair());

        // When
        var result = useCase.execute(new LoginCommand(EMAIL, PASSWORD, IP));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().accessToken()).isEqualTo("access-jwt");
        assertThat(result.getOrThrow().userId()).isEqualTo(USER_ID);
    }

    @Test
    void givenFailedAuthentication_whenAuthenticating_thenDoesNotIssueAnyToken() {
        // Given — authentication is rejected (bad password, locked, unknown email, ...)
        when(authenticator.authenticate(any(), any(), any()))
            .thenReturn(Result.failWith(ErrorCode.INVALID_CREDENTIALS));

        // When
        var result = useCase.execute(new LoginCommand(EMAIL, "wrong", IP));

        // Then — the chain short-circuits before token issuance
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_CREDENTIALS)).isTrue();
        verifyNoInteractions(tokenIssuer);
    }
}
