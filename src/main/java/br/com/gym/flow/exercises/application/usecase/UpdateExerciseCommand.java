package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.ExerciseId;
import br.com.gym.flow.exercises.domain.MuscleGroup;

public record UpdateExerciseCommand(
    ExerciseId exerciseId,
    String name,
    MuscleGroup muscleGroup,
    String description,
    String equipment,
    DifficultyLevel difficultyLevel,
    String videoUrl,
    String imageUrl,
    String actorRole
) {}
