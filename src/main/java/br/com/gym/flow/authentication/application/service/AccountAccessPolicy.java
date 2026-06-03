package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;
import br.com.gym.flow.shared.application.Policy;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import org.springframework.stereotype.Component;

/**
 * Decides which credentials status values are allowed to complete a login.
 * Pure business rule: no I/O, no side effects, no time.
 */
@Component
class AccountAccessPolicy implements Policy<UserCredentials> {

    @Override
    public Result<UserCredentials> enforce(UserCredentials candidate) {
        UserCredentialsStatus status = candidate.status();
        if (status.isBlocked()) return Result.failWith(ErrorCode.ACCOUNT_LOCKED);
        if (!status.isActive()) return Result.failWith(ErrorCode.USER_INACTIVE);
        return Result.success(candidate);
    }
}
