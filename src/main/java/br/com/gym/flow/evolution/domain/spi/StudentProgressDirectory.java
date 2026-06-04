package br.com.gym.flow.evolution.domain.spi;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * SPI exposed by the {@code evolution} module so {@code trainings} can reuse the
 * progress read model (RF-010) — activity summaries for a list of students and a
 * student's full evolution report — without reaching into {@code history}.
 * Callers are responsible for their own authorization.
 */
public interface StudentProgressDirectory {

    /** Activity summary per student, batched. */
    List<StudentActivitySummary> summariesOf(Collection<UUID> studentIds);

    /** A student's evolution indicators over the window (weekly granularity). */
    EvolutionReport reportOf(UUID studentId, Instant from, Instant to);
}
