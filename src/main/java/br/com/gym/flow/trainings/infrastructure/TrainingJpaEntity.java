package br.com.gym.flow.trainings.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trainings")
class TrainingJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "instructor_id", nullable = false)
    UUID instructorId;

    @Column(name = "name", nullable = false, length = 150)
    String name;

    @Column(name = "objective", length = 500)
    String objective;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    String status;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    Long version;

    TrainingJpaEntity() {}
}
