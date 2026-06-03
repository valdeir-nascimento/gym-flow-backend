package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.gym.flow.authentication.application.service.ActiveCredentialsLookup;
import br.com.gym.flow.authentication.application.service.RefreshTokenRotator;
import br.com.gym.flow.authentication.application.service.RefreshTokenValidator;


@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase implements CommandUseCase<RefreshTokenCommand, TokenPairView> {

    private final RefreshTokenValidator refreshTokenValidator;
    private final ActiveCredentialsLookup activeCredentialsLookup;
    private final RefreshTokenRotator refreshTokenRotator;

    @Override
    @Transactional
    public Result<TokenPairView> execute(RefreshTokenCommand command) {
        return refreshTokenValidator.validate(command.rawRefreshToken())
            .flatMap(token -> activeCredentialsLookup.findFor(token.userId())
                .map(credentials -> refreshTokenRotator.rotate(token, credentials)));
    }
}
