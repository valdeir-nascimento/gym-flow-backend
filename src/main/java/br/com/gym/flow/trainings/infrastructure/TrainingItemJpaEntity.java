package br.com.gym.flow.trainings.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "training_items")
class TrainingItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "training_id", nullable = false)
    UUID trainingId;

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

    @Column(name = "rest_seconds", nullable = false)
    int restSeconds;

    TrainingItemJpaEntity() {}
}
