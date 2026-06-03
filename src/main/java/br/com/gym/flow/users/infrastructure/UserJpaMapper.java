package br.com.gym.flow.users.infrastructure;

import br.com.gym.flow.shared.domain.Email;
import br.com.gym.flow.shared.domain.PhoneNumber;
import br.com.gym.flow.users.domain.BirthDate;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserStatus;

final class UserJpaMapper {

    private UserJpaMapper() {}

    static UserJpaEntity toEntity(User user, UserJpaEntity existing) {
        UserJpaEntity entity = existing == null ? new UserJpaEntity() : existing;
        entity.id = user.id().value();
        entity.name = user.name();
        entity.email = user.email().value();
        entity.phone = user.phone().value();
        entity.birthDate = user.birthDate().value();
        entity.role = user.role().name();
        entity.status = user.status().name();
        entity.createdBy = user.createdBy() == null ? null : user.createdBy().value();
        entity.createdAt = user.createdAt();
        entity.updatedAt = user.updatedAt();
        return entity;
    }

    static User toDomain(UserJpaEntity entity) {
        return User.hydrate(
            UserId.of(entity.id),
            entity.name,
            Email.of(entity.email),
            PhoneNumber.of(entity.phone),
            new BirthDate(entity.birthDate),
            Role.valueOf(entity.role),
            UserStatus.valueOf(entity.status),
            entity.createdBy == null ? null : UserId.of(entity.createdBy),
            entity.createdAt,
            entity.updatedAt
        );
    }
}
