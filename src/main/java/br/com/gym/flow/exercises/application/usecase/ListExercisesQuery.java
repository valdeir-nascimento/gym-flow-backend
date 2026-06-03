package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.ExerciseStatus;
import br.com.gym.flow.exercises.domain.MuscleGroup;
import org.springframework.data.domain.Pageable;

public record ListExercisesQuery(
    MuscleGroup muscleGroup,
    DifficultyLevel difficultyLevel,
    String equipment,
    String search,
    ExerciseStatus status,
    Pageable pageable
) {}
