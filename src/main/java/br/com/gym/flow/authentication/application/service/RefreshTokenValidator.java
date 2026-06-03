package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.RefreshToken;
import br.com.gym.flow.authentication.domain.RefreshTokenRepository;
import br.com.gym.flow.authentication.domain.invite.TokenHasher;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Locates a refresh token by its raw value and asserts that it is still
 * usable (not revoked, not expired). Single responsibility: the refresh
 * token lifecycle. Side-effect-free.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenValidator {

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public Result<RefreshToken> validate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return Result.failWith(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return Result.ofOptional(
                refreshTokens.findByTokenHash(TokenHasher.hash(rawRefreshToken)),
                ErrorCode.INVALID_REFRESH_TOKEN)
            .flatMap(token -> token.validate(clock).map(unused -> token));
    }
}
