package br.com.gym.flow.exercises.domain;

public enum ExerciseStatus {
    ACTIVE,
    INACTIVE;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
