package br.com.gym.flow.history.domain.spi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SPI exposed by the {@code history} module so other modules (e.g.
 * {@code evolution}) can read a student's executions within a time window —
 * to derive indicators — without depending on the aggregate or its repository.
 */
public interface WorkoutExecutionDirectory {

    List<WorkoutExecutionView> findByStudentInWindow(UUID studentId, Instant from, Instant to);

    /** Activity summary (count + last start) per student, batched (RF-010). */
    List<WorkoutActivitySummary> summariesOf(java.util.Collection<UUID> studentIds);
}
