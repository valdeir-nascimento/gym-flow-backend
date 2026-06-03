package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.MuscleGroup;

import java.util.UUID;

public record RegisterExerciseCommand(
    String name,
    MuscleGroup muscleGroup,
    String description,
    String equipment,
    DifficultyLevel difficultyLevel,
    String videoUrl,
    String imageUrl,
    UUID createdBy,
    String actorRole
) {}
