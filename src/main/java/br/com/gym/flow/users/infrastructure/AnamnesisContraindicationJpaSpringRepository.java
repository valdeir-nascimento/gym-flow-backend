package br.com.gym.flow.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface AnamnesisContraindicationJpaSpringRepository
    extends JpaRepository<AnamnesisContraindicationJpaEntity, UUID> {

    List<AnamnesisContraindicationJpaEntity> findByAnamnesisIdOrderByPosition(UUID anamnesisId);

    /** Contraindications for several revisions in one query (avoids N+1 over history). */
    List<AnamnesisContraindicationJpaEntity> findByAnamnesisIdInOrderByAnamnesisIdAscPositionAsc(
        Collection<UUID> anamnesisIds);
}
