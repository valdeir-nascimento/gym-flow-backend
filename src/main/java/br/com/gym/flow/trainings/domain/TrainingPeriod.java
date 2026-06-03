package br.com.gym.flow.trainings.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Validity window of a training. {@code endDate} may be {@code null}
 * (open-ended). Constructed only from validated input (see
 * {@link TrainingValidator}), so {@code startDate} is non-null and, when
 * present, {@code endDate} is not before it.
 */
public record TrainingPeriod(LocalDate startDate, LocalDate endDate) {

    public TrainingPeriod {
        Objects.requireNonNull(startDate, "startDate");
    }

    /** Whether {@code date} falls within this window (inclusive, open-ended when {@code endDate} is null). */
    public boolean covers(LocalDate date) {
        return !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate));
    }

    /** Whether this window and {@code other} share at least one day (null end = infinite). */
    public boolean overlaps(TrainingPeriod other) {
        boolean thisStartsWithinOther = other.endDate == null || !startDate.isAfter(other.endDate);
        boolean otherStartsWithinThis = endDate == null || !other.startDate.isAfter(endDate);
        return thisStartsWithinOther && otherStartsWithinThis;
    }
}
