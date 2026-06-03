package br.com.gym.flow.trainings.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TrainingItemJpaSpringRepository extends JpaRepository<TrainingItemJpaEntity, UUID> {

    List<TrainingItemJpaEntity> findByTrainingIdOrderByPosition(UUID trainingId);

    void deleteByTrainingId(UUID trainingId);
}
