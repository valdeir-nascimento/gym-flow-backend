package br.com.gym.flow.trainings.infrastructure;

import br.com.gym.flow.trainings.domain.Training;
import br.com.gym.flow.trainings.domain.TrainingId;
import br.com.gym.flow.trainings.domain.TrainingItem;
import br.com.gym.flow.trainings.domain.TrainingPeriod;
import br.com.gym.flow.trainings.domain.TrainingStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class TrainingJpaMapper {

    private TrainingJpaMapper() {}

    static TrainingJpaEntity toEntity(Training training, TrainingJpaEntity existing) {
        TrainingJpaEntity entity = existing == null ? new TrainingJpaEntity() : existing;
        entity.id = training.id().value();
        entity.studentId = training.studentId();
        entity.instructorId = training.instructorId();
        entity.name = training.name();
        entity.objective = training.objective();
        entity.startDate = training.period().startDate();
        entity.endDate = training.period().endDate();
        entity.status = training.status().name();
        entity.createdAt = training.createdAt();
        entity.updatedAt = training.updatedAt();
        return entity;
    }

    /** Child rows for the training, positioned by their order within the aggregate. */
    static List<TrainingItemJpaEntity> toItemEntities(Training training) {
        List<TrainingItem> items = training.items();
        List<TrainingItemJpaEntity> entities = new ArrayList<>(items.size());
        for (int position = 0; position < items.size(); position++) {
            TrainingItem item = items.get(position);
            TrainingItemJpaEntity entity = new TrainingItemJpaEntity();
            entity.id = UUID.randomUUID();
            entity.trainingId = training.id().value();
            entity.exerciseId = item.exerciseId();
            entity.position = position;
            entity.sets = item.sets();
            entity.repetitions = item.repetitions();
            entity.load = item.load();
            entity.restSeconds = item.restSeconds();
            entities.add(entity);
        }
        return entities;
    }

    /** Reconstructs the aggregate from the training row and its items (already ordered by position). */
    static Training toDomain(TrainingJpaEntity entity, List<TrainingItemJpaEntity> itemEntities) {
        List<TrainingItem> items = itemEntities.stream()
            .map(it -> new TrainingItem(it.exerciseId, it.sets, it.repetitions, it.load, it.restSeconds))
            .toList();
        return Training.hydrate(
            TrainingId.of(entity.id),
            entity.studentId,
            entity.instructorId,
            entity.name,
            entity.objective,
            new TrainingPeriod(entity.startDate, entity.endDate),
            TrainingStatus.valueOf(entity.status),
            items,
            entity.createdAt,
            entity.updatedAt);
    }
}
