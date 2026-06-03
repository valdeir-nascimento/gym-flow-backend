package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.authentication.domain.RefreshTokenRepository;
import br.com.gym.flow.authentication.domain.invite.TokenHasher;
import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class LogoutUseCase implements CommandUseCase<LogoutCommand, Void> {

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    @Override
    @Transactional
    public Result<Void> execute(LogoutCommand command) {
        String raw = command.rawRefreshToken();
        if (raw != null && !raw.isBlank()) {
            refreshTokens.findByTokenHash(TokenHasher.hash(raw))
                .ifPresent(token -> {
                    token.revoke(clock);
                    refreshTokens.save(token);
                });
        }
        return Result.ok();
    }
}
