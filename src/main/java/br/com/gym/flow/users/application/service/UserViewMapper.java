package br.com.gym.flow.users.application.service;

import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.spi.UserView;

public final class UserViewMapper {

    private UserViewMapper() {
    }

    public static UserView toView(User user) {
        return new UserView(
            user.id().value(),
            user.name(),
            user.email().value(),
            user.phone().value(),
            user.birthDate().value(),
            user.role().name(),
            user.status().name(),
            user.createdBy() == null ? null : user.createdBy().value(),
            user.createdAt(),
            user.updatedAt()
        );
    }
}
