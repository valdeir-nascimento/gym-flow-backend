package br.com.gym.flow.authentication.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

interface LoginAttemptSpringRepository extends JpaRepository<LoginAttemptJpaEntity, Long> {
    int countByEmailAndSuccessFalseAndAttemptedAtGreaterThanEqual(String email, Instant since);
}
