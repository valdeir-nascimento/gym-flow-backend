package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsStatus;

final class UserCredentialsJpaMapper {
    private UserCredentialsJpaMapper() {}

    static UserCredentialsJpaEntity toEntity(UserCredentials c, UserCredentialsJpaEntity existing) {
        UserCredentialsJpaEntity e = existing == null ? new UserCredentialsJpaEntity() : existing;
        e.userId = c.userId();
        e.email = c.email();
        e.passwordHash = c.passwordHash();
        e.passwordUpdatedAt = c.passwordUpdatedAt();
        e.role = c.role();
        e.status = c.status().name();
        return e;
    }

    static UserCredentials toDomain(UserCredentialsJpaEntity e) {
        return UserCredentials.hydrate(
            e.userId, e.email, e.passwordHash, e.passwordUpdatedAt, e.role,
            UserCredentialsStatus.valueOf(e.status));
    }
}
