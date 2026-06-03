package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.api.security.JwtProperties;
import br.com.gym.flow.api.security.JwtTokenProvider;
import br.com.gym.flow.authentication.domain.RefreshToken;
import br.com.gym.flow.authentication.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;
import br.com.gym.flow.authentication.application.usecase.TokenPairView;


/**
 * Facade that hides JWT issuance, refresh-token persistence and TTL
 * configuration behind a single collaborator. Use cases that need to
 * authenticate the user (login, refresh, consume-invite) depend on this
 * one type instead of three.
 */
@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public TokenPairView issue(UUID userId, String role) {
        var access = tokenProvider.issueAccess(userId, role);
        RefreshToken.Issued issued = RefreshToken.issue(userId, jwtProperties.refreshTtl(), clock);
        refreshTokens.save(issued.token());
        return new TokenPairView(
            access.token(),
            issued.rawToken(),
            access.expiresAt(),
            issued.token().expiresAt(),
            userId,
            role
        );
    }
}
