package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.gym.flow.authentication.application.service.InviteConsumer;
import br.com.gym.flow.authentication.application.service.PasswordInitializer;
import br.com.gym.flow.authentication.application.service.UserActivator;


/**
 * First-access flow (RF-013): consume the single-use invite, set the password
 * and activate the user. Per the spec this does NOT log the user in — it
 * returns 204 and the user authenticates separately (RF-003).
 */
@Service
@RequiredArgsConstructor
public class ConsumeInviteUseCase implements CommandUseCase<ConsumeInviteCommand, Void> {

    private final InviteConsumer inviteConsumer;
    private final PasswordInitializer passwordInitializer;
    private final UserActivator userActivator;

    @Override
    @Transactional
    public Result<Void> execute(ConsumeInviteCommand command) {
        return inviteConsumer.consume(command.rawToken())
            .flatMap(invite -> passwordInitializer.initialize(invite.userId(), command.newPassword(), command.passwordConfirmation()))
            .flatMap(userActivator::activate)
            .flatMap(credentials -> Result.ok());
    }
}
