package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.shared.application.Policy;
import br.com.gym.flow.shared.domain.Email;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Verifies that an (email, raw password, ip) triple matches an enabled
 * account. Composes the brute-force guard, the credentials lookup, the
 * access policy and the password comparison; emits the authentication
 * audit trail through {@link LoginAttemptLog}. Output is the authenticated
 * {@link UserCredentials} on success — token issuance is a separate
 * concern owned by callers.
 */
@Component
@RequiredArgsConstructor
public class EmailPasswordAuthenticator {

    private final UserCredentialsRepository credentials;
    private final BruteForceProtection bruteForce;
    private final LoginAttemptLog attemptLog;
    private final Policy<UserCredentials> accountAccessPolicy;
    private final PasswordEncoder encoder;

    public Result<UserCredentials> authenticate(String rawEmail, String rawPassword, String ipAddress) {
        return parseEmail(rawEmail)
            .flatMap(email -> bruteForce.assertNotLocked(email).map(unused -> email))
            .flatMap(email -> findOrLogMiss(email, ipAddress))
            .flatMap(accountAccessPolicy::enforce)
            .flatMap(cred -> matchOrLogMiss(cred, rawPassword, ipAddress));
    }

    private Result<String> parseEmail(String raw) {
        try {
            return Result.success(Email.of(raw).value());
        } catch (IllegalArgumentException ex) {
            return Result.failWith(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    private Result<UserCredentials> findOrLogMiss(String email, String ipAddress) {
        Optional<UserCredentials> found = credentials.findByEmail(email);
        if (found.isEmpty()) {
            attemptLog.recordFailure(email, ipAddress);
            return Result.failWith(ErrorCode.INVALID_CREDENTIALS);
        }
        return Result.success(found.get());
    }

    private Result<UserCredentials> matchOrLogMiss(UserCredentials cred, String rawPassword, String ipAddress) {
        if (!cred.matches(rawPassword, encoder)) {
            attemptLog.recordFailure(cred.email(), ipAddress);
            return Result.failWith(ErrorCode.INVALID_CREDENTIALS);
        }
        attemptLog.recordSuccess(cred.email(), ipAddress);
        return Result.success(cred);
    }
}
