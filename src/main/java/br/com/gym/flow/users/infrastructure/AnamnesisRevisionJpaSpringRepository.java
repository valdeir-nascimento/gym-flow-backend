package br.com.gym.flow.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AnamnesisRevisionJpaSpringRepository extends JpaRepository<AnamnesisRevisionJpaEntity, UUID> {

    Optional<AnamnesisRevisionJpaEntity> findFirstByStudentIdOrderByVersionDesc(UUID studentId);

    List<AnamnesisRevisionJpaEntity> findByStudentIdOrderByVersionDesc(UUID studentId);

    @Query("SELECT COALESCE(MAX(a.version), 0) FROM AnamnesisRevisionJpaEntity a WHERE a.studentId = :studentId")
    int maxVersionByStudent(@Param("studentId") UUID studentId);
}
