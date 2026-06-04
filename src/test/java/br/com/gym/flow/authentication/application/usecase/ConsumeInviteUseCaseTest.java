package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.authentication.application.service.InviteConsumer;
import br.com.gym.flow.authentication.application.service.PasswordInitializer;
import br.com.gym.flow.authentication.application.service.UserActivator;
import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;
import br.com.gym.flow.authentication.domain.invite.Invite;
import br.com.gym.flow.authentication.domain.invite.InviteId;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumeInviteUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f3");
    private static final String RAW_TOKEN = "raw-invite-token";
    private static final String PASSWORD = "Senha@1234";

    @Mock
    private InviteConsumer inviteConsumer;
    @Mock
    private PasswordInitializer passwordInitializer;
    @Mock
    private UserActivator userActivator;

    private ConsumeInviteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsumeInviteUseCase(inviteConsumer, passwordInitializer, userActivator);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static Invite invite() {
        return Invite.hydrate(InviteId.newId(), USER_ID, "hash",
            Instant.parse("2026-06-04T12:00:00Z"), null, Instant.parse("2026-06-01T12:00:00Z"));
    }

    private static UserCredentials activatedCredentials() {
        return UserCredentials.hydrate(USER_ID, "maria@example.com", "enc:Senha@1234",
            Instant.parse("2026-06-01T12:00:00Z"), "STUDENT", UserCredentialsStatus.ACTIVE);
    }

    private ConsumeInviteCommand command() {
        return new ConsumeInviteCommand(RAW_TOKEN, PASSWORD, PASSWORD, "203.0.113.7");
    }

    @Test
    void givenValidInvite_whenConsuming_thenInitializesAndActivatesWithoutLoggingIn() {
        // Given — the whole chain succeeds
        UserCredentials cred = activatedCredentials();
        when(inviteConsumer.consume(RAW_TOKEN)).thenReturn(Result.success(invite()));
        when(passwordInitializer.initialize(USER_ID, PASSWORD, PASSWORD)).thenReturn(Result.success(cred));
        when(userActivator.activate(cred)).thenReturn(Result.success(cred));

        // When
        var result = useCase.execute(command());

        // Then — success carries no tokens (RF-013: user authenticates separately)
        assertThat(result.isSuccess()).isTrue();
        verify(userActivator).activate(cred);
    }

    @Test
    void givenInvalidInvite_whenConsuming_thenStopsBeforeTouchingPasswordOrActivation() {
        // Given — invite is expired/consumed/unknown
        when(inviteConsumer.consume(any())).thenReturn(Result.failWith(ErrorCode.INVITE_TOKEN_EXPIRED));

        // When
        var result = useCase.execute(command());

        // Then — nothing downstream runs
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVITE_TOKEN_EXPIRED)).isTrue();
        verifyNoInteractions(passwordInitializer, userActivator);
    }

    @Test
    void givenPasswordMismatch_whenConsuming_thenStopsBeforeActivation() {
        // Given — invite is fine, but the password initialization fails
        when(inviteConsumer.consume(RAW_TOKEN)).thenReturn(Result.success(invite()));
        when(passwordInitializer.initialize(any(), any(), any()))
            .thenReturn(Result.failWith(ErrorCode.PASSWORD_MISMATCH));

        // When
        var result = useCase.execute(command());

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.PASSWORD_MISMATCH)).isTrue();
        verifyNoInteractions(userActivator);
    }
}
