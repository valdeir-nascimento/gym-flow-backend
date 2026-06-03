package br.com.gym.flow.users.domain.bond;

import java.util.Objects;
import java.util.UUID;

public record BondId(UUID value) {
    public BondId {
        Objects.requireNonNull(value, "value");
    }

    public static BondId newId() {
        return new BondId(UUID.randomUUID());
    }

    public static BondId of(UUID value) {
        return new BondId(value);
    }
}
