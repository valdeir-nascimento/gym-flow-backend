package br.com.gym.flow.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface HealthDataConsentJpaSpringRepository extends JpaRepository<HealthDataConsentJpaEntity, UUID> {

    boolean existsByStudentId(UUID studentId);

    Optional<HealthDataConsentJpaEntity> findByStudentId(UUID studentId);
}
