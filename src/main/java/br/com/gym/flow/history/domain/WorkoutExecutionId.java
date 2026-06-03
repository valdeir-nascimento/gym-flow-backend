package br.com.gym.flow.history.domain;

import java.util.Objects;
import java.util.UUID;

public record WorkoutExecutionId(UUID value) {

    public WorkoutExecutionId {
        Objects.requireNonNull(value, "value");
    }

    public static WorkoutExecutionId newId() {
        return new WorkoutExecutionId(UUID.randomUUID());
    }

    public static WorkoutExecutionId of(UUID value) {
        return new WorkoutExecutionId(value);
    }

    public static WorkoutExecutionId of(String value) {
        return new WorkoutExecutionId(UUID.fromString(value));
    }
}
