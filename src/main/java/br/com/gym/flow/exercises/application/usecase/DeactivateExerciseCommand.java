package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.domain.ExerciseId;

public record DeactivateExerciseCommand(ExerciseId exerciseId, String actorRole) {}
