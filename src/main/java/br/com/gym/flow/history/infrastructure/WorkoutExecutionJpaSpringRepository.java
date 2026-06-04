package br.com.gym.flow.history.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface WorkoutExecutionJpaSpringRepository extends JpaRepository<WorkoutExecutionJpaEntity, UUID> {

    boolean existsByStudentIdAndTrainingIdAndStartedAt(UUID studentId, UUID trainingId, Instant startedAt);

    /** A student's executions in a started-at window, oldest first (RF-008). */
    List<WorkoutExecutionJpaEntity> findByStudentIdAndStartedAtBetweenOrderByStartedAtAsc(
        UUID studentId, Instant from, Instant to);

    /** Count and latest start per student, in one query (RF-010). */
    @Query("""
        SELECT e.studentId, COUNT(e), MAX(e.startedAt)
        FROM WorkoutExecutionJpaEntity e
        WHERE e.studentId IN :studentIds
        GROUP BY e.studentId
        """)
    List<Object[]> aggregateActivityByStudent(@Param("studentIds") Collection<UUID> studentIds);

    /**
     * History search (RF-009): always scoped to the student; the remaining filters
     * are optional (null = no restriction). The exercise filter matches executions
     * that performed it (EXISTS over the items). Ordering comes from the Pageable.
     */
    @Query(value = """
        SELECT e FROM WorkoutExecutionJpaEntity e
        WHERE e.studentId = :studentId
          AND (:trainingId IS NULL OR e.trainingId = :trainingId)
          AND (:startedFrom IS NULL OR e.startedAt >= :startedFrom)
          AND (:startedTo IS NULL OR e.startedAt <= :startedTo)
          AND (:exerciseId IS NULL OR EXISTS (
              SELECT 1 FROM WorkoutExecutionItemJpaEntity i
              WHERE i.executionId = e.id AND i.exerciseId = :exerciseId))
        """,
        countQuery = """
        SELECT COUNT(e) FROM WorkoutExecutionJpaEntity e
        WHERE e.studentId = :studentId
          AND (:trainingId IS NULL OR e.trainingId = :trainingId)
          AND (:startedFrom IS NULL OR e.startedAt >= :startedFrom)
          AND (:startedTo IS NULL OR e.startedAt <= :startedTo)
          AND (:exerciseId IS NULL OR EXISTS (
              SELECT 1 FROM WorkoutExecutionItemJpaEntity i
              WHERE i.executionId = e.id AND i.exerciseId = :exerciseId))
        """)
    Page<WorkoutExecutionJpaEntity> search(
        @Param("studentId") UUID studentId,
        @Param("trainingId") UUID trainingId,
        @Param("exerciseId") UUID exerciseId,
        @Param("startedFrom") Instant startedFrom,
        @Param("startedTo") Instant startedTo,
        Pageable pageable);
}
