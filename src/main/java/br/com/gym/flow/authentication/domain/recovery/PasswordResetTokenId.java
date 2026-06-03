package br.com.gym.flow.authentication.domain.recovery;

import java.util.Objects;
import java.util.UUID;

public record PasswordResetTokenId(UUID value) {
    public PasswordResetTokenId { Objects.requireNonNull(value, "value"); }
    public static PasswordResetTokenId newId() { return new PasswordResetTokenId(UUID.randomUUID()); }
    public static PasswordResetTokenId of(UUID v) { return new PasswordResetTokenId(v); }
}
