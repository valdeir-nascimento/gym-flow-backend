package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.PwnedPasswordChecker;
import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/**
 * Establishes the initial password for a user just past first access:
 * checks confirmation match, applies the password policy and writes the
 * hash. Single responsibility: the password initialization rule.
 */
@Component
@RequiredArgsConstructor
public class PasswordInitializer {

    private final UserCredentialsRepository credentials;
    private final PasswordEncoder encoder;
    private final PwnedPasswordChecker pwned;
    private final Clock clock;

    public Result<UserCredentials> initialize(UUID userId, String newPassword, String confirmation) {
        if (newPassword == null || !newPassword.equals(confirmation)) {
            return Result.failWith(ErrorCode.PASSWORD_MISMATCH);
        }
        return Result.ofOptional(credentials.findByUserId(userId), ErrorCode.USER_NOT_FOUND)
            .flatMap(cred -> cred.defineInitialPassword(newPassword, pwned, encoder, clock)
                .map(unused -> cred));
    }
}
