package br.com.gym.flow.authentication.domain.invite;

import java.util.Optional;
import java.util.UUID;

public interface InviteRepository {

    Invite save(Invite invite);

    Optional<Invite> findByTokenHash(String tokenHash);

    Optional<Invite> findActiveByUserId(UUID userId);
}
