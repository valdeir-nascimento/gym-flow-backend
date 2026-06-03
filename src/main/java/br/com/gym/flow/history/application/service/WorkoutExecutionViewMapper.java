package br.com.gym.flow.history.application.service;

import br.com.gym.flow.history.domain.WorkoutExecution;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionItemView;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;

public final class WorkoutExecutionViewMapper {

    private WorkoutExecutionViewMapper() {
    }

    public static WorkoutExecutionView toView(final WorkoutExecution execution) {
        var items = execution.items().stream()
            .map(item -> new WorkoutExecutionItemView(
                item.exerciseId(), item.sets(), item.repetitions(), item.load(), item.notes()))
            .toList();
        return new WorkoutExecutionView(
            execution.id().value(),
            execution.studentId(),
            execution.trainingId(),
            execution.startedAt(),
            execution.finishedAt(),
            execution.notes(),
            items,
            execution.registeredAt()
        );
    }
}
