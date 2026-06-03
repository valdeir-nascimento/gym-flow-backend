package br.com.gym.flow.history.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_executions")
class WorkoutExecutionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "training_id", nullable = false)
    UUID trainingId;

    @Column(name = "started_at", nullable = false)
    Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    Instant finishedAt;

    @Column(name = "notes", length = 1000)
    String notes;

    @Column(name = "registered_at", nullable = false)
    Instant registeredAt;

    WorkoutExecutionJpaEntity() {
    }
}
