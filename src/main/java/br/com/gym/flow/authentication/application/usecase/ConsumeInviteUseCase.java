package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.gym.flow.authentication.application.service.InviteConsumer;
import br.com.gym.flow.authentication.application.service.PasswordInitializer;
import br.com.gym.flow.authentication.application.service.TokenIssuer;
import br.com.gym.flow.authentication.application.service.UserActivator;


@Service
@RequiredArgsConstructor
public class ConsumeInviteUseCase implements CommandUseCase<ConsumeInviteCommand, TokenPairView> {

    private final InviteConsumer inviteConsumer;
    private final PasswordInitializer passwordInitializer;
    private final UserActivator userActivator;
    private final TokenIssuer tokenIssuer;

    @Override
    @Transactional
    public Result<TokenPairView> execute(ConsumeInviteCommand command) {
        return inviteConsumer.consume(command.rawToken())
            .flatMap(invite -> passwordInitializer.initialize(invite.userId(), command.newPassword(), command.passwordConfirmation()))
            .flatMap(userActivator::activate)
            .map(cred -> tokenIssuer.issue(cred.userId(), cred.role()));
    }
}
