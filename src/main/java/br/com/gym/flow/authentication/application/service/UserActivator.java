package br.com.gym.flow.authentication.application.service;

import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.spi.ChangeUserStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Activates a user across module boundaries: drives the SPI exposed by
 * {@code users} (which publishes {@code UserActivated}) and mirrors the
 * resulting status on the local credentials projection. Single
 * responsibility: the cross-module status transition.
 */
@Component
@RequiredArgsConstructor
public class UserActivator {

    private final ChangeUserStatusPort userStatus;
    private final UserCredentialsRepository credentials;

    public Result<UserCredentials> activate(UserCredentials cred) {
        return userStatus.activate(cred.userId())
            .map(unused -> {
                cred.updateStatus(UserCredentialsStatus.ACTIVE);
                return credentials.save(cred);
            });
    }
}
