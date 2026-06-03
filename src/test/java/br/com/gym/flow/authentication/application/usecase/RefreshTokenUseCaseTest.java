package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.authentication.application.service.ActiveCredentialsLookup;
import br.com.gym.flow.authentication.application.service.RefreshTokenRotator;
import br.com.gym.flow.authentication.application.service.RefreshTokenValidator;
import br.com.gym.flow.authentication.domain.RefreshToken;
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
class RefreshTokenUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f2");
    private static final String RAW_REFRESH = "raw-refresh-token";

    @Mock
    private RefreshTokenValidator refreshTokenValidator;
    @Mock
    private ActiveCredentialsLookup activeCredentialsLookup;
    @Mock
    private RefreshTokenRotator refreshTokenRotator;

    private RefreshTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCase(refreshTokenValidator, activeCredentialsLookup, refreshTokenRotator);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static RefreshToken validToken() {
        return RefreshToken.hydrate(UUID.randomUUID(), USER_ID, "hash",
            Instant.parse("2026-06-08T12:00:00Z"), null, Instant.parse("2026-06-01T12:00:00Z"));
    }

    private static UserCredentials activeCredentials() {
        return UserCredentials.hydrate(USER_ID, "maria@example.com", "enc:x",
            Instant.parse("2026-01-01T00:00:00Z"), "STUDENT", UserCredentialsStatus.ACTIVE);
    }

    private static TokenPairView tokenPair() {
        return new TokenPairView("new-access", "new-refresh",
            Instant.parse("2026-06-01T12:15:00Z"), Instant.parse("2026-06-08T12:00:00Z"), USER_ID, "STUDENT");
    }

    @Test
    void givenValidToken_whenRefreshing_thenRotatesIntoANewPair() {
        // Given
        RefreshToken token = validToken();
        UserCredentials cred = activeCredentials();
        when(refreshTokenValidator.validate(RAW_REFRESH)).thenReturn(Result.success(token));
        when(activeCredentialsLookup.findFor(USER_ID)).thenReturn(Result.success(cred));
        when(refreshTokenRotator.rotate(token, cred)).thenReturn(tokenPair());

        // When
        var result = useCase.execute(new RefreshTokenCommand(RAW_REFRESH));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().accessToken()).isEqualTo("new-access");
    }

    @Test
    void givenInvalidToken_whenRefreshing_thenStopsBeforeLookupAndRotation() {
        // Given
        when(refreshTokenValidator.validate(any()))
            .thenReturn(Result.failWith(ErrorCode.INVALID_REFRESH_TOKEN));

        // When
        var result = useCase.execute(new RefreshTokenCommand(RAW_REFRESH));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_REFRESH_TOKEN)).isTrue();
        verifyNoInteractions(activeCredentialsLookup, refreshTokenRotator);
    }

    @Test
    void givenValidTokenButInactiveUser_whenRefreshing_thenStopsBeforeRotation() {
        // Given — token is fine, but the account is no longer allowed to authenticate
        when(refreshTokenValidator.validate(RAW_REFRESH)).thenReturn(Result.success(validToken()));
        when(activeCredentialsLookup.findFor(USER_ID)).thenReturn(Result.failWith(ErrorCode.USER_INACTIVE));

        // When
        var result = useCase.execute(new RefreshTokenCommand(RAW_REFRESH));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_INACTIVE)).isTrue();
        verifyNoInteractions(refreshTokenRotator);
    }
}
