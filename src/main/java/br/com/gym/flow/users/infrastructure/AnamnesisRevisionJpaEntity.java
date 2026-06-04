package br.com.gym.flow.users.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anamnesis_revisions")
class AnamnesisRevisionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "version", nullable = false)
    int version;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    BigDecimal weightKg;

    @Column(name = "height_cm", nullable = false)
    int heightCm;

    @Column(name = "objectives", length = 2000)
    String objectives;

    @Column(name = "conditioning_history", length = 2000)
    String conditioningHistory;

    // Free-text lists, one entry per line (never queried structurally).
    @Column(name = "injuries", length = 2000)
    String injuries;

    @Column(name = "medical_restrictions", length = 2000)
    String medicalRestrictions;

    @Column(name = "observations", length = 2000)
    String observations;

    @Column(name = "created_by", nullable = false)
    UUID createdBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    AnamnesisRevisionJpaEntity() {
    }
}
