package br.com.gym.flow.history.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workout_execution_items")
class WorkoutExecutionItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "execution_id", nullable = false)
    UUID executionId;

    @Column(name = "exercise_id", nullable = false)
    UUID exerciseId;

    @Column(name = "position", nullable = false)
    int position;

    @Column(name = "sets", nullable = false)
    int sets;

    @Column(name = "repetitions", nullable = false)
    int repetitions;

    @Column(name = "load_kg", precision = 7, scale = 2)
    BigDecimal load;

    @Column(name = "notes", length = 500)
    String notes;

    WorkoutExecutionItemJpaEntity() {
    }
}
