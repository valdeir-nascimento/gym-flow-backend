package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.authentication.domain.PwnedPasswordChecker;
import br.com.gym.flow.authentication.domain.RefreshTokenRepository;
import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.authentication.domain.invite.TokenHasher;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeOwnPasswordUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f5");
    private static final String CURRENT_PASSWORD = "OldPass@99";
    private static final String NEW_PASSWORD = "NewSenha@123";
    private static final String REFRESH_TOKEN = "raw-refresh-token";

    private static final PwnedPasswordChecker NOT_PWNED = raw -> false;
    private static final PasswordEncoder ENCODER = new PasswordEncoder() {
        @Override
        public String encode(CharSequence raw) {
            return "enc:" + raw;
        }

        @Override
        public boolean matches(CharSequence raw, String encoded) {
            return encoded != null && encoded.equals("enc:" + raw);
        }
    };

    @Mock
    private UserCredentialsRepository credentials;
    @Mock
    private RefreshTokenRepository refreshTokens;

    private ChangeOwnPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeOwnPasswordUseCase(credentials, refreshTokens, ENCODER, NOT_PWNED, CLOCK);
    }

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static UserCredentials credentials() {
        return UserCredentials.hydrate(USER_ID, "maria@example.com", "enc:" + CURRENT_PASSWORD, NOW,
            "STUDENT", UserCredentialsStatus.ACTIVE);
    }

    private static ChangeOwnPasswordCommand command(final String current, final String next, final String confirmation) {
        return new ChangeOwnPasswordCommand(USER_ID, current, next, confirmation, REFRESH_TOKEN);
    }

    @Test
    void givenMismatchedConfirmation_whenChanging_thenFailsAndTouchesNothing() {
        // When
        var result = useCase.execute(command(CURRENT_PASSWORD, NEW_PASSWORD, "Different@123"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.PASSWORD_MISMATCH)).isTrue();
        verifyNoInteractions(credentials, refreshTokens);
    }

    @Test
    void givenWrongCurrentPassword_whenChanging_thenFailsUnauthorized() {
        // Given
        when(credentials.findByUserId(USER_ID)).thenReturn(Optional.of(credentials()));

        // When — current password is wrong
        var result = useCase.execute(command("WrongPass@1", NEW_PASSWORD, NEW_PASSWORD));

        // Then — 401, nothing persisted or revoked
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_CREDENTIALS)).isTrue();
        verify(credentials, never()).save(any());
        verifyNoInteractions(refreshTokens);
    }

    @Test
    void givenNewPasswordEqualToCurrent_whenChanging_thenFailsBusinessRule() {
        // Given
        when(credentials.findByUserId(USER_ID)).thenReturn(Optional.of(credentials()));

        // When — new equals current
        var result = useCase.execute(command(CURRENT_PASSWORD, CURRENT_PASSWORD, CURRENT_PASSWORD));

        // Then — 422
        assertThat(failureOf(result).hasAnyCode(ErrorCode.WEAK_PASSWORD)).isTrue();
        verify(credentials, never()).save(any());
        verifyNoInteractions(refreshTokens);
    }

    @Test
    void givenValidChange_whenChanging_thenPersistsAndRevokesOtherSessions() {
        // Given
        UserCredentials cred = credentials();
        when(credentials.findByUserId(USER_ID)).thenReturn(Optional.of(cred));

        // When
        var result = useCase.execute(command(CURRENT_PASSWORD, NEW_PASSWORD, NEW_PASSWORD));

        // Then — succeeds; the current session is preserved, the others revoked
        assertThat(result.isSuccess()).isTrue();
        verify(credentials).save(cred);
        verify(refreshTokens).revokeAllByUserIdExcept(USER_ID, TokenHasher.hash(REFRESH_TOKEN));
    }
}
