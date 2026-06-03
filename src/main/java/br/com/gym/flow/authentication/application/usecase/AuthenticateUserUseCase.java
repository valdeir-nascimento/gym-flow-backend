package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.gym.flow.authentication.application.service.EmailPasswordAuthenticator;
import br.com.gym.flow.authentication.application.service.TokenIssuer;


@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase implements CommandUseCase<LoginCommand, TokenPairView> {

    private final EmailPasswordAuthenticator emailPasswordAuthenticator;
    private final TokenIssuer tokenIssuer;

    @Override
    @Transactional
    public Result<TokenPairView> execute(LoginCommand command) {
        return emailPasswordAuthenticator
            .authenticate(command.email(), command.rawPassword(), command.ipAddress())
            .map(cred -> tokenIssuer.issue(cred.userId(), cred.role()));
    }
}
