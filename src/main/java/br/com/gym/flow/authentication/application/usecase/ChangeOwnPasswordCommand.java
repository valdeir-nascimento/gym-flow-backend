package br.com.gym.flow.authentication.application.usecase;

import java.util.UUID;

/**
 * A user's intention to change their own password (RF-015). {@code userId} is
 * the authenticated caller; {@code currentRefreshToken} identifies the current
 * session, which is preserved while the other sessions are revoked.
 */
public record ChangeOwnPasswordCommand(
    UUID userId,
    String currentPassword,
    String newPassword,
    String passwordConfirmation,
    String currentRefreshToken
) {}
