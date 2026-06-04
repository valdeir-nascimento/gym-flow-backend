package br.com.gym.flow.users.domain.anamnesis;

import java.util.Objects;
import java.util.UUID;

public record AnamnesisId(UUID value) {

    public AnamnesisId {
        Objects.requireNonNull(value, "value");
    }

    public static AnamnesisId newId() {
        return new AnamnesisId(UUID.randomUUID());
    }

    public static AnamnesisId of(UUID value) {
        return new AnamnesisId(value);
    }
}
