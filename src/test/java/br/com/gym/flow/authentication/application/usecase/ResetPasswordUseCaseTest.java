package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.authentication.domain.PwnedPasswordChecker;
import br.com.gym.flow.authentication.domain.RefreshTokenRepository;
import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;
import br.com.gym.flow.authentication.domain.recovery.PasswordResetToken;
import br.com.gym.flow.authentication.domain.recovery.PasswordResetTokenId;
import br.com.gym.flow.authentication.domain.recovery.PasswordResetTokenRepository;
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
class ResetPasswordUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f4");
    private static final String RAW_TOKEN = "raw-reset-token";
    private static final String STRONG_PASSWORD = "NewSenha@123";

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
    private PasswordResetTokenRepository tokens;
    @Mock
    private UserCredentialsRepository credentials;
    @Mock
    private RefreshTokenRepository refreshTokens;

    private ResetPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResetPasswordUseCase(tokens, credentials, refreshTokens, ENCODER, NOT_PWNED, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static PasswordResetToken usableToken() {
        return PasswordResetToken.hydrate(PasswordResetTokenId.newId(), USER_ID, "hash",
            NOW.plusSeconds(3600), null, NOW.minusSeconds(60));
    }

    private static UserCredentials credentialsWith(UserCredentialsStatus status) {
        return UserCredentials.hydrate(USER_ID, "maria@example.com", "enc:OldPass@99", NOW, "STUDENT", status);
    }

    @Test
    void givenMismatchedConfirmation_whenResetting_thenFailsAndTouchesNothing() {
        // When
        var result = useCase.execute(new ResetPasswordCommand(RAW_TOKEN, STRONG_PASSWORD, "Different@123"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.PASSWORD_MISMATCH)).isTrue();
        verifyNoInteractions(tokens, credentials, refreshTokens);
    }

    @Test
    void givenUnknownToken_whenResetting_thenFailsInvalidToken() {
        // Given
        when(tokens.findByTokenHash(any())).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new ResetPasswordCommand(RAW_TOKEN, STRONG_PASSWORD, STRONG_PASSWORD));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_PASSWORD_RESET_TOKEN)).isTrue();
        verify(credentials, never()).save(any());
    }

    @Test
    void givenExpiredToken_whenResetting_thenFailsExpiredAndDoesNotChangePassword() {
        // Given — token found, but past its expiry
        PasswordResetToken expired = PasswordResetToken.hydrate(
            PasswordResetTokenId.newId(), USER_ID, "hash", NOW.minusSeconds(60), null, NOW.minusSeconds(3600));
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(expired));

        // When
        var result = useCase.execute(new ResetPasswordCommand(RAW_TOKEN, STRONG_PASSWORD, STRONG_PASSWORD));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED)).isTrue();
        verifyNoInteractions(credentials, refreshTokens);
    }

    @Test
    void givenValidTokenButMissingCredentials_whenResetting_thenFailsNotFound() {
        // Given
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(usableToken()));
        when(credentials.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new ResetPasswordCommand(RAW_TOKEN, STRONG_PASSWORD, STRONG_PASSWORD));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_FOUND)).isTrue();
        verify(credentials, never()).save(any());
    }

    @Test
    void givenWeakNewPassword_whenResetting_thenPropagatesPolicyFailure() {
        // Given — confirmation matches, token & user are fine, but the password is weak
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(usableToken()));
        when(credentials.findByUserId(USER_ID)).thenReturn(Optional.of(credentialsWith(UserCredentialsStatus.ACTIVE)));

        // When
        var result = useCase.execute(new ResetPasswordCommand(RAW_TOKEN, "weak", "weak"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.WEAK_PASSWORD)).isTrue();
        verify(credentials, never()).save(any());
        verifyNoInteractions(refreshTokens);
    }

    @Test
    void givenBlockedUserAndValidReset_whenResetting_thenChangesPasswordUnblocksAndRevokesSessions() {
        // Given — a blocked account performing a valid password reset
        UserCredentials cred = credentialsWith(UserCredentialsStatus.BLOCKED);
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(usableToken()));
        when(credentials.findByUserId(USER_ID)).thenReturn(Optional.of(cred));

        // When
        var result = useCase.execute(new ResetPasswordCommand(RAW_TOKEN, STRONG_PASSWORD, STRONG_PASSWORD));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(cred.matches(STRONG_PASSWORD, ENCODER)).isTrue();
        assertThat(cred.status()).isEqualTo(UserCredentialsStatus.ACTIVE); // unblocked
        verify(credentials).save(cred);
        verify(refreshTokens).revokeAllByUserId(USER_ID); // all existing sessions killed
    }
}
