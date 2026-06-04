package br.com.gym.flow.users.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "anamnesis_contraindications")
class AnamnesisContraindicationJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "anamnesis_id", nullable = false)
    UUID anamnesisId;

    @Column(name = "exercise_id", nullable = false)
    UUID exerciseId;

    @Column(name = "position", nullable = false)
    int position;

    AnamnesisContraindicationJpaEntity() {
    }
}
