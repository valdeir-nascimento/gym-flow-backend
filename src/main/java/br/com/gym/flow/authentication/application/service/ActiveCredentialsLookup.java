package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.shared.application.Policy;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves credentials for a user id, gated by a {@link Policy} that
 * decides whether the account is currently allowed to authenticate.
 * Used by flows that re-validate an existing session (refresh, etc.)
 * and need to make sure the user did not become inactive or blocked
 * since the previous login.
 */
@Component
@RequiredArgsConstructor
public class ActiveCredentialsLookup {

    private final UserCredentialsRepository credentials;
    private final Policy<UserCredentials> accountAccessPolicy;

    public Result<UserCredentials> findFor(UUID userId) {
        return Result.ofOptional(credentials.findByUserId(userId), ErrorCode.USER_NOT_FOUND)
            .flatMap(accountAccessPolicy::enforce);
    }
}
