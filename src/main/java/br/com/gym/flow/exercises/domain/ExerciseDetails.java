package br.com.gym.flow.exercises.domain;

public record ExerciseDetails(
    String name,
    MuscleGroup muscleGroup,
    String description,
    String equipment,
    DifficultyLevel difficultyLevel,
    String videoUrl,
    String imageUrl
) {
}
