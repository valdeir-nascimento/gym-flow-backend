package br.com.gym.flow.exercises.presentation;

import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.MuscleGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterExerciseRequest(
    @NotBlank String name,
    @NotNull MuscleGroup muscleGroup,
    String description,
    String equipment,
    @NotNull DifficultyLevel difficultyLevel,
    String videoUrl,
    String imageUrl
) {}
