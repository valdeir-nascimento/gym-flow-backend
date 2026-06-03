package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.invite.Invite;
import br.com.gym.flow.authentication.domain.invite.InviteRepository;
import br.com.gym.flow.authentication.domain.invite.TokenHasher;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Locates an invite by its raw token, validates that it is still usable
 * and marks it consumed. Single responsibility: the invite lifecycle.
 */
@Component
@RequiredArgsConstructor
public class InviteConsumer {

    private final InviteRepository invites;
    private final Clock clock;

    public Result<Invite> consume(String rawToken) {
        return Result.ofOptional(
                invites.findByTokenHash(TokenHasher.hash(rawToken)),
                ErrorCode.INVALID_INVITE_TOKEN)
            .flatMap(invite -> invite.consume(clock).map(unused -> invite))
            .map(invites::save);
    }
}
