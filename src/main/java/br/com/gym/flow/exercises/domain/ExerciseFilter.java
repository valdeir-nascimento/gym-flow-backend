package br.com.gym.flow.exercises.domain;

public record ExerciseFilter(
    MuscleGroup muscleGroup,
    DifficultyLevel difficultyLevel,
    String equipment,
    String search,
    ExerciseStatus status
) {}
