package br.com.gym.flow.history.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface WorkoutExecutionItemJpaSpringRepository extends JpaRepository<WorkoutExecutionItemJpaEntity, UUID> {
}
