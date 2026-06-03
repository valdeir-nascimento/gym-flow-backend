package br.com.gym.flow.exercises.application.service;

import br.com.gym.flow.exercises.domain.Exercise;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;

public final class ExerciseViewMapper {

    private ExerciseViewMapper() {}

    public static ExerciseView toView(Exercise exercise) {
        return new ExerciseView(
            exercise.id().value(),
            exercise.name(),
            exercise.muscleGroup().name(),
            exercise.description(),
            exercise.equipment(),
            exercise.difficultyLevel().name(),
            exercise.videoUrl(),
            exercise.imageUrl(),
            exercise.status().name(),
            exercise.createdBy(),
            exercise.createdAt(),
            exercise.updatedAt());
    }
}
