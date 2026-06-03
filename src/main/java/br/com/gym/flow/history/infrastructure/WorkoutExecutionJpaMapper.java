package br.com.gym.flow.history.infrastructure;

import br.com.gym.flow.history.domain.ExecutedExercise;
import br.com.gym.flow.history.domain.WorkoutExecution;
import br.com.gym.flow.history.domain.WorkoutExecutionId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class WorkoutExecutionJpaMapper {

    private WorkoutExecutionJpaMapper() {
    }

    static WorkoutExecutionJpaEntity toEntity(final WorkoutExecution execution) {
        WorkoutExecutionJpaEntity entity = new WorkoutExecutionJpaEntity();
        entity.id = execution.id().value();
        entity.studentId = execution.studentId();
        entity.trainingId = execution.trainingId();
        entity.startedAt = execution.startedAt();
        entity.finishedAt = execution.finishedAt();
        entity.notes = execution.notes();
        entity.registeredAt = execution.registeredAt();
        return entity;
    }

    /** Child rows for the execution, positioned by their order within the aggregate. */
    static List<WorkoutExecutionItemJpaEntity> toItemEntities(final WorkoutExecution execution) {
        List<ExecutedExercise> items = execution.items();
        List<WorkoutExecutionItemJpaEntity> entities = new ArrayList<>(items.size());
        for (int position = 0; position < items.size(); position++) {
            ExecutedExercise item = items.get(position);
            WorkoutExecutionItemJpaEntity entity = new WorkoutExecutionItemJpaEntity();
            entity.id = UUID.randomUUID();
            entity.executionId = execution.id().value();
            entity.exerciseId = item.exerciseId();
            entity.position = position;
            entity.sets = item.sets();
            entity.repetitions = item.repetitions();
            entity.load = item.load();
            entity.notes = item.notes();
            entities.add(entity);
        }
        return entities;
    }

    /** Reconstructs the aggregate from the execution row and its items (already ordered by position). */
    static WorkoutExecution toDomain(
        final WorkoutExecutionJpaEntity entity,
        final List<WorkoutExecutionItemJpaEntity> itemEntities
    ) {
        List<ExecutedExercise> items = itemEntities.stream()
            .map(it -> new ExecutedExercise(it.exerciseId, it.sets, it.repetitions, it.load, it.notes))
            .toList();
        return WorkoutExecution.hydrate(
            WorkoutExecutionId.of(entity.id),
            entity.studentId,
            entity.trainingId,
            entity.startedAt,
            entity.finishedAt,
            entity.notes,
            items,
            entity.registeredAt
        );
    }
}
