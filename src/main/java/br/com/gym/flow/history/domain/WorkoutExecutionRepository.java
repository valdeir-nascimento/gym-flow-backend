package br.com.gym.flow.history.domain;

import java.time.Instant;
import java.util.UUID;

public interface WorkoutExecutionRepository {

    WorkoutExecution save(WorkoutExecution execution);

    /**
     * Whether the idempotency key (student + training + start instant) is already
     * taken (RF-007): a re-submission of the same execution must not duplicate it.
     */
    boolean existsByStudentAndTrainingAndStart(UUID studentId, UUID trainingId, Instant startedAt);
}
