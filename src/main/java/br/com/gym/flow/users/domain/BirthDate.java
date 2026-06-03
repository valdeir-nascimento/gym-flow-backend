package br.com.gym.flow.users.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

public record BirthDate(LocalDate value) {

    static final int MIN_AGE_YEARS = 13;

    public BirthDate {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Parses and validates a candidate birth date against the system clock.
     * Throws {@link IllegalArgumentException} if the candidate is null, in
     * the future, or refers to someone younger than the minimum age. Same
     * contract as {@link br.com.gym.flow.shared.domain.Email#of(String)}
     * and {@link br.com.gym.flow.shared.domain.PhoneNumber#of(String)}.
     */
    public static BirthDate of(final LocalDate candidate, final Clock clock) {
        if (!isValid(candidate, LocalDate.now(clock))) {
            throw new IllegalArgumentException("invalid birth date: " + candidate);
        }
        return new BirthDate(candidate);
    }

    public static boolean isValid(final LocalDate candidate, final LocalDate today) {
        if (candidate == null || today == null) return false;
        if (!candidate.isBefore(today)) return false;
        return Period.between(candidate, today).getYears() >= MIN_AGE_YEARS;
    }
}
