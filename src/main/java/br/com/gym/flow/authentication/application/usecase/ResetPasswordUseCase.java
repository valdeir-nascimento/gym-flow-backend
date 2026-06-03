package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.authentication.domain.PwnedPasswordChecker;
import br.com.gym.flow.authentication.domain.RefreshTokenRepository;
import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;
import br.com.gym.flow.authentication.domain.invite.TokenHasher;
import br.com.gym.flow.authentication.domain.recovery.PasswordResetToken;
import br.com.gym.flow.authentication.domain.recovery.PasswordResetTokenRepository;
import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase implements CommandUseCase<ResetPasswordCommand, Void> {

    private final PasswordResetTokenRepository tokens;
    private final UserCredentialsRepository credentials;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final PwnedPasswordChecker pwned;
    private final Clock clock;

    @Override
    @Transactional
    public Result<Void> execute(ResetPasswordCommand command) {
        if (command.newPassword() == null || !command.newPassword().equals(command.passwordConfirmation())) {
            return Result.failWith(ErrorCode.PASSWORD_MISMATCH);
        }

        Optional<PasswordResetToken> maybe = tokens.findByTokenHash(TokenHasher.hash(command.rawToken()));
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
        PasswordResetToken token = maybe.get();

        Result<Void> consume = token.consume(clock);
        if (!consume.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) consume).notification());
        }

        Optional<UserCredentials> credsMaybe = credentials.findByUserId(token.userId());
        if (credsMaybe.isEmpty()) {
            return Result.failWith(ErrorCode.USER_NOT_FOUND);
        }
        UserCredentials cred = credsMaybe.get();

        Result<Void> changed = cred.changePassword(command.newPassword(), pwned, encoder, clock);
        if (!changed.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) changed).notification());
        }

        if (cred.status().isBlocked()) {
            cred.updateStatus(UserCredentialsStatus.ACTIVE);
        }
        credentials.save(cred);
        tokens.save(token);
        refreshTokens.revokeAllByUserId(cred.userId());
        return Result.ok();
    }
}
