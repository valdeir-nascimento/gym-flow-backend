package br.com.gym.flow.authentication.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface InviteSpringRepository extends JpaRepository<InviteJpaEntity, UUID> {
    Optional<InviteJpaEntity> findByTokenHash(String tokenHash);

    Optional<InviteJpaEntity> findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId);
}
