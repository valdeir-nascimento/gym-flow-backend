package br.com.gym.flow.trainings.infrastructure;

import br.com.gym.flow.trainings.domain.Training;
import br.com.gym.flow.trainings.domain.TrainingId;
import br.com.gym.flow.trainings.domain.TrainingPeriod;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Page<Training> findByStudentId(UUID studentId, Pageable pageable) {
        Page<TrainingJpaEntity> page = trainings.findByStudentId(studentId, pageable);
        if (page.isEmpty()) {
            return page.map(entity -> TrainingJpaMapper.toDomain(entity, List.of()));
        }

        // Batch-load the items for the whole page, then group by training (no N+1).
        List<UUID> ids = page.map(entity -> entity.id).getContent();
        Map<UUID, List<TrainingItemJpaEntity>> itemsByTraining =
            items.findByTrainingIdInOrderByTrainingIdAscPositionAsc(ids).stream()
                .collect(Collectors.groupingBy(item -> item.trainingId));

        return page.map(entity ->
            TrainingJpaMapper.toDomain(entity, itemsByTraining.getOrDefault(entity.id, List.of())));
    }

    @Override
    public boolean existsActiveOverlapping(UUID studentId, TrainingPeriod period) {
        return trainings.existsActiveOverlapping(studentId, period.startDate(), period.endDate());
    }
}
