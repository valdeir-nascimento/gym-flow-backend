package br.com.gym.flow.history.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

interface WorkoutExecutionJpaSpringRepository extends JpaRepository<WorkoutExecutionJpaEntity, UUID> {

    boolean existsByStudentIdAndTrainingIdAndStartedAt(UUID studentId, UUID trainingId, Instant startedAt);
}
