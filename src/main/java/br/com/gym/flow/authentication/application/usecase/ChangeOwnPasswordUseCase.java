package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.authentication.domain.PwnedPasswordChecker;
import br.com.gym.flow.authentication.domain.RefreshTokenRepository;
import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.authentication.domain.invite.TokenHasher;
import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Self-service password change (RF-015): verifies the current password, applies
 * the new one under the password policy, and revokes the user's other sessions
 * while preserving the current refresh token.
 */
@Service
@RequiredArgsConstructor
public class ChangeOwnPasswordUseCase implements CommandUseCase<ChangeOwnPasswordCommand, Void> {

    private final UserCredentialsRepository credentials;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final PwnedPasswordChecker pwned;
    private final Clock clock;

    @Override
    @Transactional
    public Result<Void> execute(final ChangeOwnPasswordCommand command) {
        // Confirmation must match (400).
        if (command.newPassword() == null || !command.newPassword().equals(command.passwordConfirmation())) {
            return Result.failWith(ErrorCode.PASSWORD_MISMATCH);
        }

        Optional<UserCredentials> maybe = credentials.findByUserId(command.userId());
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.USER_NOT_FOUND);
        }
        UserCredentials cred = maybe.get();

        // Current password must be correct (401, without revealing which field).
        if (!cred.matches(command.currentPassword(), encoder)) {
            return Result.failWith(ErrorCode.INVALID_CREDENTIALS);
        }

        // Apply the new password under the policy: weak/pwned/same-as-current -> 422.
        Result<Void> changed = cred.changePassword(command.newPassword(), pwned, encoder, clock);
        if (!changed.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) changed).notification());
        }

        credentials.save(cred);
        // Invalidate the other sessions, keeping the current one (RF-015).
        refreshTokens.revokeAllByUserIdExcept(command.userId(), TokenHasher.hash(command.currentRefreshToken()));
        return Result.ok();
    }
}
