package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.LoginAttempt;
import br.com.gym.flow.authentication.domain.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
@RequiredArgsConstructor
class LoginAttemptRepositoryAdapter implements LoginAttemptRepository {

    private final LoginAttemptSpringRepository jpa;

    @Override
    public void record(LoginAttempt attempt) {
        LoginAttemptJpaEntity entity = new LoginAttemptJpaEntity();
        entity.email = attempt.email();
        entity.attemptedAt = attempt.attemptedAt();
        entity.success = attempt.success();
        entity.ipAddress = attempt.ipAddress();
        jpa.save(entity);
    }

    @Override
    public int countFailuresSince(String email, Instant since) {
        return jpa.countByEmailAndSuccessFalseAndAttemptedAtGreaterThanEqual(email, since);
    }
}
