package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.application.usecase.TokenPairView;
import br.com.gym.flow.authentication.domain.RefreshToken;
import br.com.gym.flow.authentication.domain.RefreshTokenRepository;
import br.com.gym.flow.authentication.domain.UserCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Revokes the consumed refresh token and issues a fresh access/refresh
 * pair in a single step. Single responsibility: refresh token rotation.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenRotator {

    private final RefreshTokenRepository refreshTokens;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    public TokenPairView rotate(RefreshToken consumed, UserCredentials credentials) {
        consumed.revoke(clock);
        refreshTokens.save(consumed);
        return tokenIssuer.issue(credentials.userId(), credentials.role());
    }
}
