package br.com.gym.flow.users.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "health_data_consents")
class HealthDataConsentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "granted_by", nullable = false)
    UUID grantedBy;

    @Column(name = "granted_at", nullable = false)
    Instant grantedAt;

    HealthDataConsentJpaEntity() {
    }
}
