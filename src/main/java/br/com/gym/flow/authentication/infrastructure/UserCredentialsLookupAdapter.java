package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.spi.UserCredentialsLookupPort;
import br.com.gym.flow.authentication.domain.spi.UserCredentialsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class UserCredentialsLookupAdapter implements UserCredentialsLookupPort {

    private final UserCredentialsSpringRepository jpa;

    @Override
    public Optional<UserCredentialsView> findByEmail(String email) {
        return jpa.findByEmail(email).map(this::toView);
    }

    @Override
    public Optional<UserCredentialsView> findByUserId(UUID userId) {
        return jpa.findById(userId).map(this::toView);
    }

    private UserCredentialsView toView(UserCredentialsJpaEntity e) {
        return new UserCredentialsView(e.userId, e.email, e.passwordHash, e.role, e.status);
    }
}
