package br.com.gym.flow.authentication.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.authentication.application.usecase.AuthenticateUserUseCase;
import br.com.gym.flow.authentication.application.usecase.ConsumeInviteCommand;
import br.com.gym.flow.authentication.application.usecase.ConsumeInviteUseCase;
import br.com.gym.flow.authentication.application.usecase.LoginCommand;
import br.com.gym.flow.authentication.application.usecase.LogoutCommand;
import br.com.gym.flow.authentication.application.usecase.LogoutUseCase;
import br.com.gym.flow.authentication.application.usecase.RecoveryRequestCommand;
import br.com.gym.flow.authentication.application.usecase.RefreshTokenCommand;
import br.com.gym.flow.authentication.application.usecase.RefreshTokenUseCase;
import br.com.gym.flow.authentication.application.usecase.RequestPasswordRecoveryUseCase;
import br.com.gym.flow.authentication.application.usecase.ResetPasswordCommand;
import br.com.gym.flow.authentication.application.usecase.ResetPasswordUseCase;
import br.com.gym.flow.authentication.application.usecase.TokenPairView;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.observability.CustomerContextFilter;
import br.com.gym.flow.shared.observability.MdcRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exclude the custom OncePerRequestFilter @Components (would fail to instantiate in the slice);
// addFilters = false keeps the security chain from being applied. /auth itself is permitAll in prod.
@WebMvcTest(controllers = AuthController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c9");
    private static final String EMAIL = "maria@example.com";
    private static final String PASSWORD = "Senha@1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUser;
    @MockitoBean
    private RefreshTokenUseCase refreshToken;
    @MockitoBean
    private LogoutUseCase logout;
    @MockitoBean
    private ConsumeInviteUseCase consumeInvite;
    @MockitoBean
    private RequestPasswordRecoveryUseCase requestPasswordRecovery;
    @MockitoBean
    private ResetPasswordUseCase resetPassword;

    private static TokenPairView tokenPair() {
        return new TokenPairView("access-jwt", "raw-refresh",
            Instant.parse("2026-06-01T12:15:00Z"), Instant.parse("2026-06-08T12:00:00Z"), USER_ID, "STUDENT");
    }

    private String json(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Nested
    class Login {

        @Test
        void givenValidCredentials_whenLoggingIn_thenReturns200BodyAndMapsCommand() throws Exception {
            // Given
            when(authenticateUser.execute(any())).thenReturn(Result.success(tokenPair()));

            // When / Then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("raw-refresh"))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.role").value("STUDENT"));

            // Then — request mapped into the command
            var captor = ArgumentCaptor.forClass(LoginCommand.class);
            verify(authenticateUser).execute(captor.capture());
            assertThat(captor.getValue().email()).isEqualTo(EMAIL);
            assertThat(captor.getValue().rawPassword()).isEqualTo(PASSWORD);
            verifyNoMoreInteractions(authenticateUser);
        }

        @Test
        void givenWrongCredentials_whenLoggingIn_thenReturns401() throws Exception {
            // Given
            when(authenticateUser.execute(any())).thenReturn(Result.failWith(ErrorCode.INVALID_CREDENTIALS));

            // When / Then — UNAUTHORIZED category -> 401
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new LoginRequest(EMAIL, "wrong"))))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void givenTooManyAttempts_whenLoggingIn_thenReturns401() throws Exception {
            // Given
            when(authenticateUser.execute(any())).thenReturn(Result.failWith(ErrorCode.LOGIN_THROTTLED));

            // When / Then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void givenBlankEmail_whenLoggingIn_thenReturns400AndSkipsUseCase() throws Exception {
            // When / Then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new LoginRequest("", PASSWORD))))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(authenticateUser);
        }
    }

    @Nested
    class Refresh {

        @Test
        void givenValidToken_whenRefreshing_thenReturns200AndMapsCommand() throws Exception {
            // Given
            when(refreshToken.execute(any())).thenReturn(Result.success(tokenPair()));

            // When / Then
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new RefreshRequest("raw-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-jwt"));

            var captor = ArgumentCaptor.forClass(RefreshTokenCommand.class);
            verify(refreshToken).execute(captor.capture());
            assertThat(captor.getValue().rawRefreshToken()).isEqualTo("raw-refresh");
            verifyNoMoreInteractions(refreshToken);
        }

        @Test
        void givenInvalidToken_whenRefreshing_thenReturns401() throws Exception {
            // Given
            when(refreshToken.execute(any())).thenReturn(Result.failWith(ErrorCode.INVALID_REFRESH_TOKEN));

            // When / Then
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new RefreshRequest("stale"))))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void givenBlankToken_whenRefreshing_thenReturns400() throws Exception {
            // When / Then
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new RefreshRequest(""))))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(refreshToken);
        }
    }

    @Nested
    class Logout {

        @Test
        void givenValidToken_whenLoggingOut_thenReturns204AndMapsCommand() throws Exception {
            // Given
            when(logout.execute(any())).thenReturn(Result.ok());

            // When / Then — NO_CONTENT
            mockMvc.perform(post("/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new RefreshRequest("raw-refresh"))))
                .andExpect(status().isNoContent());

            var captor = ArgumentCaptor.forClass(LogoutCommand.class);
            verify(logout).execute(captor.capture());
            assertThat(captor.getValue().rawRefreshToken()).isEqualTo("raw-refresh");
            verifyNoMoreInteractions(logout);
        }

        @Test
        void givenBlankToken_whenLoggingOut_thenReturns400() throws Exception {
            // When / Then
            mockMvc.perform(post("/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new RefreshRequest(""))))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(logout);
        }
    }

    @Nested
    class ConsumeInvite {

        @Test
        void givenValidInvite_whenConsuming_thenReturns204AndMapsTokenFromPath() throws Exception {
            // Given — RF-013: first access does not auto-login
            when(consumeInvite.execute(any())).thenReturn(Result.ok());

            // When / Then
            mockMvc.perform(post("/auth/invites/{token}/consume", "invite-token-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new ConsumeInviteRequest(PASSWORD, PASSWORD))))
                .andExpect(status().isNoContent());

            // Then — path token + body mapped into the command
            var captor = ArgumentCaptor.forClass(ConsumeInviteCommand.class);
            verify(consumeInvite).execute(captor.capture());
            assertThat(captor.getValue().rawToken()).isEqualTo("invite-token-123");
            assertThat(captor.getValue().newPassword()).isEqualTo(PASSWORD);
            assertThat(captor.getValue().passwordConfirmation()).isEqualTo(PASSWORD);
            verifyNoMoreInteractions(consumeInvite);
        }

        @Test
        void givenExpiredInvite_whenConsuming_thenReturns410() throws Exception {
            // Given — single-use token expired -> 410 Gone (RF-013)
            when(consumeInvite.execute(any())).thenReturn(Result.failWith(ErrorCode.INVITE_TOKEN_EXPIRED));

            // When / Then
            mockMvc.perform(post("/auth/invites/{token}/consume", "expired")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new ConsumeInviteRequest(PASSWORD, PASSWORD))))
                .andExpect(status().isGone());
        }

        @Test
        void givenBlankPassword_whenConsuming_thenReturns400() throws Exception {
            // When / Then
            mockMvc.perform(post("/auth/invites/{token}/consume", "invite-token-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new ConsumeInviteRequest("", PASSWORD))))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(consumeInvite);
        }
    }

    @Nested
    class RequestRecovery {

        @Test
        void givenEmail_whenRequestingRecovery_thenReturns202AndMapsCommand() throws Exception {
            // Given
            when(requestPasswordRecovery.execute(any())).thenReturn(Result.ok());

            // When / Then — ACCEPTED (recovery is processed regardless of whether the email exists)
            mockMvc.perform(post("/auth/password-recovery")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new RecoveryRequest(EMAIL))))
                .andExpect(status().isAccepted());

            var captor = ArgumentCaptor.forClass(RecoveryRequestCommand.class);
            verify(requestPasswordRecovery).execute(captor.capture());
            assertThat(captor.getValue().rawEmail()).isEqualTo(EMAIL);
            verifyNoMoreInteractions(requestPasswordRecovery);
        }

        @Test
        void givenBlankEmail_whenRequestingRecovery_thenReturns400() throws Exception {
            // When / Then
            mockMvc.perform(post("/auth/password-recovery")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new RecoveryRequest(""))))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(requestPasswordRecovery);
        }
    }

    @Nested
    class ResetPassword {

        @Test
        void givenValidReset_whenResetting_thenReturns204AndMapsTokenFromPath() throws Exception {
            // Given
            when(resetPassword.execute(any())).thenReturn(Result.ok());

            // When / Then — NO_CONTENT
            mockMvc.perform(post("/auth/password-reset/{token}", "reset-token-456")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new ResetPasswordRequest(PASSWORD, PASSWORD))))
                .andExpect(status().isNoContent());

            var captor = ArgumentCaptor.forClass(ResetPasswordCommand.class);
            verify(resetPassword).execute(captor.capture());
            assertThat(captor.getValue().rawToken()).isEqualTo("reset-token-456");
            assertThat(captor.getValue().newPassword()).isEqualTo(PASSWORD);
            verifyNoMoreInteractions(resetPassword);
        }

        @Test
        void givenPasswordMismatch_whenResetting_thenReturns400() throws Exception {
            // Given — VALIDATION category -> 400
            when(resetPassword.execute(any())).thenReturn(Result.failWith(ErrorCode.PASSWORD_MISMATCH));

            // When / Then
            mockMvc.perform(post("/auth/password-reset/{token}", "reset-token-456")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new ResetPasswordRequest(PASSWORD, "Different@123"))))
                .andExpect(status().isBadRequest());
        }

        @Test
        void givenInvalidToken_whenResetting_thenReturns410() throws Exception {
            // Given — single-use reset token invalid -> 410 Gone (RF-014)
            when(resetPassword.execute(any())).thenReturn(Result.failWith(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

            // When / Then
            mockMvc.perform(post("/auth/password-reset/{token}", "bad")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(new ResetPasswordRequest(PASSWORD, PASSWORD))))
                .andExpect(status().isGone());
        }
    }
}
