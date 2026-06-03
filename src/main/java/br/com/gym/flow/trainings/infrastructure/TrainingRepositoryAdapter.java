package br.com.gym.flow.trainings.infrastructure;

import br.com.gym.flow.trainings.domain.Training;
import br.com.gym.flow.trainings.domain.TrainingId;
import br.com.gym.flow.trainings.domain.TrainingPeriod;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class TrainingRepositoryAdapter implements TrainingRepository {

    private final TrainingJpaSpringRepository trainings;
    private final TrainingItemJpaSpringRepository items;

    @Override
    public Training save(Training training) {
        TrainingJpaEntity existing = trainings.findById(training.id().value()).orElse(null);
        TrainingJpaEntity savedTraining = trainings.save(TrainingJpaMapper.toEntity(training, existing));

        // The aggregate owns its items: replace the child rows with the current set.
        List<TrainingItemJpaEntity> itemEntities = TrainingJpaMapper.toItemEntities(training);
        items.deleteByTrainingId(training.id().value());
        items.saveAll(itemEntities);

        return TrainingJpaMapper.toDomain(savedTraining, itemEntities);
    }

    @Override
    public Optional<Training> findById(TrainingId id) {
        return trainings.findById(id.value())
            .map(entity -> TrainingJpaMapper.toDomain(entity, items.findByTrainingIdOrderByPosition(id.value())));
    }

    @Override
    public boolean existsActiveOverlapping(UUID studentId, TrainingPeriod period) {
        return trainings.existsActiveOverlapping(studentId, period.startDate(), period.endDate());
    }
}
