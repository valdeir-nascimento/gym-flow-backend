package br.com.gym.flow.history.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutExecutionRepository {

    WorkoutExecution save(WorkoutExecution execution);

    /**
     * Whether the idempotency key (student + training + start instant) is already
     * taken (RF-007): a re-submission of the same execution must not duplicate it.
     */
    boolean existsByStudentAndTrainingAndStart(UUID studentId, UUID trainingId, Instant startedAt);

    /** A single execution with its items (RF-009 detail). */
    Optional<WorkoutExecution> findById(WorkoutExecutionId id);

    /** A student's activity history matching {@code filter}, paginated (RF-009). */
    Page<WorkoutExecution> search(WorkoutExecutionFilter filter, Pageable pageable);

    /**
     * All of a student's executions whose start falls in [{@code from}, {@code to}],
     * oldest first (RF-008: feeds the evolution indicators). Not paginated — the
     * window is bounded by the caller (≤ 24 months).
     */
    List<WorkoutExecution> findByStudentInWindow(UUID studentId, Instant from, Instant to);
}
