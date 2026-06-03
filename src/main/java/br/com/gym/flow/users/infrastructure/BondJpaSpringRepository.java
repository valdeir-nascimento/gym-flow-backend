package br.com.gym.flow.users.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface BondJpaSpringRepository extends JpaRepository<BondJpaEntity, UUID> {

    Optional<BondJpaEntity> findByStudentIdAndEndedAtIsNull(UUID studentId);

    Page<BondJpaEntity> findByInstructorId(UUID instructorId, Pageable pageable);

    Page<BondJpaEntity> findByStudentId(UUID studentId, Pageable pageable);
}
