package br.com.gym.flow.users.domain.anamnesis;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Body weight in kilograms (RF-017). Guards a plausible human range; out-of-range
 * values are rejected so the use case can surface a 422.
 */
public record Weight(BigDecimal kilograms) {

    private static final BigDecimal MIN_KG = new BigDecimal("20");
    private static final BigDecimal MAX_KG = new BigDecimal("400");

    public Weight {
        Objects.requireNonNull(kilograms, "kilograms");
    }

    public static Weight of(final BigDecimal kilograms) {
        if (kilograms == null || kilograms.compareTo(MIN_KG) < 0 || kilograms.compareTo(MAX_KG) > 0) {
            throw new IllegalArgumentException("implausible weight: " + kilograms);
        }
        return new Weight(kilograms);
    }
}
