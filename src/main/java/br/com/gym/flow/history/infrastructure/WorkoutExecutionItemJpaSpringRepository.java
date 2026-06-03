package br.com.gym.flow.history.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface WorkoutExecutionItemJpaSpringRepository extends JpaRepository<WorkoutExecutionItemJpaEntity, UUID> {

    List<WorkoutExecutionItemJpaEntity> findByExecutionIdOrderByPosition(UUID executionId);

    /** Items for several executions in a single query (RF-009: avoids N+1 over a page). */
    List<WorkoutExecutionItemJpaEntity> findByExecutionIdInOrderByExecutionIdAscPositionAsc(Collection<UUID> executionIds);
}
