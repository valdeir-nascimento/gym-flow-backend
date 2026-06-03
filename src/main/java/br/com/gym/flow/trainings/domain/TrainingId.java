package br.com.gym.flow.trainings.domain;

import java.util.Objects;
import java.util.UUID;

public record TrainingId(UUID value) {

    public TrainingId {
        Objects.requireNonNull(value, "value");
    }

    public static TrainingId newId() {
        return new TrainingId(UUID.randomUUID());
    }

    public static TrainingId of(UUID value) {
        return new TrainingId(value);
    }

    public static TrainingId of(String value) {
        return new TrainingId(UUID.fromString(value));
    }
}
