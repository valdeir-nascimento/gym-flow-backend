package br.com.gym.flow.authentication.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserCredentialsSpringRepository extends JpaRepository<UserCredentialsJpaEntity, UUID> {
    Optional<UserCredentialsJpaEntity> findByEmail(String email);
}
