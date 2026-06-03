package br.com.gym.flow.trainings.application.service;

import br.com.gym.flow.trainings.domain.Training;
import br.com.gym.flow.trainings.domain.spi.TrainingItemView;
import br.com.gym.flow.trainings.domain.spi.TrainingView;

public final class TrainingViewMapper {

    private TrainingViewMapper() {}

    public static TrainingView toView(Training training) {
        var items = training.items().stream()
            .map(item -> new TrainingItemView(
                item.exerciseId(), item.sets(), item.repetitions(), item.load(), item.restSeconds()))
            .toList();
        return new TrainingView(
            training.id().value(),
            training.studentId(),
            training.instructorId(),
            training.name(),
            training.objective(),
            training.period().startDate(),
            training.period().endDate(),
            training.status().name(),
            items,
            training.createdAt(),
            training.updatedAt());
    }
}
