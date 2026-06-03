package br.com.gym.flow.trainings.domain;

public enum TrainingStatus {
    ACTIVE,
    ARCHIVED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
