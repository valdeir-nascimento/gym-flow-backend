package br.com.gym.flow.authentication.domain;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    int revokeAllByUserId(UUID userId);

    /** Revokes the user's active refresh tokens except the one with {@code keepTokenHash} (RF-015). */
    int revokeAllByUserIdExcept(UUID userId, String keepTokenHash);
}
