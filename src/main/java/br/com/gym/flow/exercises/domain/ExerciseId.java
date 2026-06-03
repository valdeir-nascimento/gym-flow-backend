package br.com.gym.flow.exercises.domain;

import java.util.Objects;
import java.util.UUID;

public record ExerciseId(UUID value) {

    public ExerciseId {
        Objects.requireNonNull(value, "value");
    }

    public static ExerciseId newId() {
        return new ExerciseId(UUID.randomUUID());
    }

    public static ExerciseId of(UUID value) {
        return new ExerciseId(value);
    }

    public static ExerciseId of(String value) {
        return new ExerciseId(UUID.fromString(value));
    }
}
