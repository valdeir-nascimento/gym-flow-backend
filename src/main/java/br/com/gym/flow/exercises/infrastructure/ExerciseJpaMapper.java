package br.com.gym.flow.exercises.infrastructure;

import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.Exercise;
import br.com.gym.flow.exercises.domain.ExerciseDetails;
import br.com.gym.flow.exercises.domain.ExerciseId;
import br.com.gym.flow.exercises.domain.ExerciseStatus;
import br.com.gym.flow.exercises.domain.MuscleGroup;

final class ExerciseJpaMapper {

    private ExerciseJpaMapper() {}

    static ExerciseJpaEntity toEntity(Exercise exercise, ExerciseJpaEntity existing) {
        ExerciseJpaEntity entity = existing == null ? new ExerciseJpaEntity() : existing;
        entity.id = exercise.id().value();
        entity.name = exercise.name();
        entity.muscleGroup = exercise.muscleGroup().name();
        entity.description = exercise.description();
        entity.equipment = exercise.equipment();
        entity.difficultyLevel = exercise.difficultyLevel().name();
        entity.videoUrl = exercise.videoUrl();
        entity.imageUrl = exercise.imageUrl();
        entity.status = exercise.status().name();
        entity.createdBy = exercise.createdBy();
        entity.createdAt = exercise.createdAt();
        entity.updatedAt = exercise.updatedAt();
        return entity;
    }

    static Exercise toDomain(ExerciseJpaEntity entity) {
        ExerciseDetails details = new ExerciseDetails(
            entity.name,
            MuscleGroup.valueOf(entity.muscleGroup),
            entity.description,
            entity.equipment,
            DifficultyLevel.valueOf(entity.difficultyLevel),
            entity.videoUrl,
            entity.imageUrl);
        return Exercise.hydrate(
            ExerciseId.of(entity.id),
            details,
            ExerciseStatus.valueOf(entity.status),
            entity.createdBy,
            entity.createdAt,
            entity.updatedAt);
    }
}
