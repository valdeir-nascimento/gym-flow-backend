package br.com.gym.flow.authentication.presentation;

import br.com.gym.flow.shared.web.ClientIp;
import br.com.gym.flow.authentication.application.usecase.AuthenticateUserUseCase;
import br.com.gym.flow.authentication.application.usecase.ChangeOwnPasswordCommand;
import br.com.gym.flow.authentication.application.usecase.ChangeOwnPasswordUseCase;
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
import br.com.gym.flow.shared.domain.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController implements AuthApi {

    private final AuthenticateUserUseCase authenticateUser;
    private final RefreshTokenUseCase refreshToken;
    private final LogoutUseCase logout;
    private final ConsumeInviteUseCase consumeInvite;
    private final RequestPasswordRecoveryUseCase requestPasswordRecovery;
    private final ResetPasswordUseCase resetPassword;
    private final ChangeOwnPasswordUseCase changeOwnPassword;

    @Override
    @PostMapping("/login")
    public Result<TokenPairView> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return authenticateUser.execute(
            new LoginCommand(req.email(), req.password(), ClientIp.resolve(http)));
    }

    @Override
    @PostMapping("/refresh")
    public Result<TokenPairView> refresh(@Valid @RequestBody RefreshRequest req) {
        return refreshToken.execute(new RefreshTokenCommand(req.refreshToken()));
    }

    @Override
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Result<Void> logout(@Valid @RequestBody RefreshRequest req) {
        return logout.execute(new LogoutCommand(req.refreshToken()));
    }

    @Override
    @PostMapping("/invites/{token}/consume")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Result<Void> consumeInvite(@PathVariable String token,
                                      @Valid @RequestBody ConsumeInviteRequest req,
                                      HttpServletRequest http) {
        return consumeInvite.execute(new ConsumeInviteCommand(
            token, req.newPassword(), req.passwordConfirmation(), ClientIp.resolve(http)));
    }

    @Override
    @PostMapping("/password-recovery")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<Void> requestRecovery(@Valid @RequestBody RecoveryRequest req) {
        return requestPasswordRecovery.execute(new RecoveryRequestCommand(req.email()));
    }

    @Override
    @PostMapping("/password-reset/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Result<Void> resetPassword(@PathVariable String token,
                                      @Valid @RequestBody ResetPasswordRequest req) {
        return resetPassword.execute(new ResetPasswordCommand(
            token, req.newPassword(), req.passwordConfirmation()));
    }

    @Override
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                       @RequestHeader("X-User-Id") UUID userId) {
        return changeOwnPassword.execute(new ChangeOwnPasswordCommand(
            userId, req.currentPassword(), req.newPassword(), req.passwordConfirmation(), req.currentRefreshToken()));
    }
}
