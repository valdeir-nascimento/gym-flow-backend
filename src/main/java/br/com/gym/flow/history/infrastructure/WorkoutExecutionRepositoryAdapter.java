package br.com.gym.flow.history.infrastructure;

import br.com.gym.flow.history.domain.WorkoutExecution;
import br.com.gym.flow.history.domain.WorkoutExecutionFilter;
import br.com.gym.flow.history.domain.WorkoutExecutionId;
import br.com.gym.flow.history.domain.WorkoutExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    public Optional<WorkoutExecution> findById(final WorkoutExecutionId id) {
        return executions.findById(id.value())
            .map(entity -> WorkoutExecutionJpaMapper.toDomain(
                entity, items.findByExecutionIdOrderByPosition(id.value())));
    }

    @Override
    public Page<WorkoutExecution> search(final WorkoutExecutionFilter filter, final Pageable pageable) {
        Page<WorkoutExecutionJpaEntity> page = executions.search(
            filter.studentId(), filter.trainingId(), filter.exerciseId(),
            filter.startedFrom(), filter.startedTo(), pageable);
        Map<UUID, List<WorkoutExecutionItemJpaEntity>> itemsByExecution =
            itemsFor(page.map(entity -> entity.id).getContent());
        return page.map(entity ->
            WorkoutExecutionJpaMapper.toDomain(entity, itemsByExecution.getOrDefault(entity.id, List.of())));
    }

    @Override
    public List<WorkoutExecution> findByStudentInWindow(final UUID studentId, final Instant from, final Instant to) {
        List<WorkoutExecutionJpaEntity> rows =
            executions.findByStudentIdAndStartedAtBetweenOrderByStartedAtAsc(studentId, from, to);
        Map<UUID, List<WorkoutExecutionItemJpaEntity>> itemsByExecution =
            itemsFor(rows.stream().map(entity -> entity.id).toList());
        return rows.stream()
            .map(entity -> WorkoutExecutionJpaMapper.toDomain(entity, itemsByExecution.getOrDefault(entity.id, List.of())))
            .toList();
    }

    /** Batch-loads the items for several executions, grouped by execution id (no N+1). */
    private Map<UUID, List<WorkoutExecutionItemJpaEntity>> itemsFor(final List<UUID> executionIds) {
        if (executionIds.isEmpty()) {
            return Map.of();
        }
        return items.findByExecutionIdInOrderByExecutionIdAscPositionAsc(executionIds).stream()
            .collect(Collectors.groupingBy(item -> item.executionId));
    }
}
