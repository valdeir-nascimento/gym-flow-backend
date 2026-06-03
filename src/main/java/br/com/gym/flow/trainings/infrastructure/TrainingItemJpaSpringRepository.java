package br.com.gym.flow.trainings.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface TrainingItemJpaSpringRepository extends JpaRepository<TrainingItemJpaEntity, UUID> {

    List<TrainingItemJpaEntity> findByTrainingIdOrderByPosition(UUID trainingId);

    /** Items for several trainings in a single query (RF-006: avoids N+1 over a page). */
    List<TrainingItemJpaEntity> findByTrainingIdInOrderByTrainingIdAscPositionAsc(Collection<UUID> trainingIds);

    void deleteByTrainingId(UUID trainingId);
}
