package br.com.gym.flow.authentication.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialsRepository {

    UserCredentials save(UserCredentials credentials);

    Optional<UserCredentials> findByUserId(UUID userId);

    Optional<UserCredentials> findByEmail(String email);
}
