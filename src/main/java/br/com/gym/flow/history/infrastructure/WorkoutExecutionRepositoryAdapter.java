package br.com.gym.flow.history.infrastructure;

import br.com.gym.flow.history.domain.WorkoutExecution;
import br.com.gym.flow.history.domain.WorkoutExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class WorkoutExecutionRepositoryAdapter implements WorkoutExecutionRepository {

    private final WorkoutExecutionJpaSpringRepository executions;
    private final WorkoutExecutionItemJpaSpringRepository items;

    @Override
    public WorkoutExecution save(final WorkoutExecution execution) {
        // The execution is immutable: it is only ever inserted once, together
        // with its items (idempotency is guarded before reaching this point).
        WorkoutExecutionJpaEntity savedExecution = executions.save(WorkoutExecutionJpaMapper.toEntity(execution));
        List<WorkoutExecutionItemJpaEntity> itemEntities = WorkoutExecutionJpaMapper.toItemEntities(execution);
        items.saveAll(itemEntities);
        return WorkoutExecutionJpaMapper.toDomain(savedExecution, itemEntities);
    }

    @Override
    public boolean existsByStudentAndTrainingAndStart(
        final UUID studentId,
        final UUID trainingId,
        final Instant startedAt
    ) {
        return executions.existsByStudentIdAndTrainingIdAndStartedAt(studentId, trainingId, startedAt);
    }
}
