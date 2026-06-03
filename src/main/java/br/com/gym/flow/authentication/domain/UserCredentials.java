package br.com.gym.flow.authentication.domain;

import br.com.gym.flow.shared.domain.AggregateRoot;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
public class UserCredentials extends AggregateRoot<UUID> {

    private final String email;
    private String passwordHash;
    private Instant passwordUpdatedAt;
    private String role;
    private UserCredentialsStatus status;

    private UserCredentials(UUID userId, String email, String passwordHash,
                            Instant passwordUpdatedAt, String role, UserCredentialsStatus status) {
        super(userId);
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordUpdatedAt = passwordUpdatedAt;
        this.role = role;
        this.status = status;
    }

    public static UserCredentials hydrate(UUID userId, String email, String passwordHash,
                                          Instant passwordUpdatedAt, String role, UserCredentialsStatus status) {
        return new UserCredentials(userId, email, passwordHash, passwordUpdatedAt, role, status);
    }

    public static UserCredentials pending(UUID userId, String email, String role) {
        return new UserCredentials(userId, email, null, null, role, UserCredentialsStatus.PENDING_FIRST_ACCESS);
    }

    public Result<Void> defineInitialPassword(String rawPassword,
                                              PwnedPasswordChecker pwned,
                                              PasswordEncoder encoder,
                                              Clock clock) {
        if (this.passwordHash != null) {
            return Result.failWith(ErrorCode.USER_NOT_PENDING_FIRST_ACCESS);
        }
        Result<Void> policy = PasswordPolicy.validate(rawPassword, pwned);
        if (!policy.isSuccess()) return policy;
        this.passwordHash = encoder.encode(rawPassword);
        this.passwordUpdatedAt = Instant.now(clock);
        return Result.ok();
    }

    public Result<Void> changePassword(String rawPassword,
                                        PwnedPasswordChecker pwned,
                                        PasswordEncoder encoder,
                                        Clock clock) {
        Result<Void> policy = PasswordPolicy.validate(rawPassword, pwned);
        if (!policy.isSuccess()) return policy;
        if (this.passwordHash != null && encoder.matches(rawPassword, this.passwordHash)) {
            return Result.failWith(ErrorCode.WEAK_PASSWORD, "a nova senha não pode ser igual à anterior");
        }
        this.passwordHash = encoder.encode(rawPassword);
        this.passwordUpdatedAt = Instant.now(clock);
        return Result.ok();
    }

    public boolean matches(String rawPassword, PasswordEncoder encoder) {
        return passwordHash != null && encoder.matches(rawPassword, passwordHash);
    }

    public void updateStatus(UserCredentialsStatus status) { this.status = status; }
    public void updateRole(String role) { this.role = role; }

    public UUID userId() { return id(); }
}
