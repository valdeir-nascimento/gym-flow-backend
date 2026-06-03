package br.com.gym.flow.users.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teacher_student_bonds")
class BondJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "instructor_id", nullable = false)
    UUID instructorId;

    @Column(name = "started_at", nullable = false)
    Instant startedAt;

    @Column(name = "ended_at")
    Instant endedAt;

    @Column(name = "created_by", nullable = false)
    UUID createdBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    /**
     * Optimistic-lock token managed by Hibernate. Lives only on the persistence
     * model — the {@code TeacherStudentBond} aggregate stays version-agnostic.
     * A {@code null} value marks a not-yet-persisted row, letting Spring Data
     * {@code persist} on insert instead of {@code merge}.
     */
    @Version
    @Column(name = "version", nullable = false)
    Long version;

    BondJpaEntity() {}
}
