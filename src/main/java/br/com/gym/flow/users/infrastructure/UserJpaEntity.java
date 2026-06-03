package br.com.gym.flow.users.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "name", nullable = false, length = 150)
    String name;

    @Column(name = "email", nullable = false, length = 255)
    String email;

    @Column(name = "phone", nullable = false, length = 20)
    String phone;

    @Column(name = "birth_date", nullable = false)
    LocalDate birthDate;

    @Column(name = "role", nullable = false, length = 20)
    String role;

    @Column(name = "status", nullable = false, length = 30)
    String status;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    /**
     * Optimistic-lock token managed by Hibernate. Lives only on the persistence
     * model — the {@code User} aggregate stays version-agnostic. A {@code null}
     * value marks a not-yet-persisted row, letting Spring Data {@code persist}
     * on insert instead of {@code merge} (skips the pre-insert SELECT).
     */
    @Version
    @Column(name = "version", nullable = false)
    Long version;

    UserJpaEntity() {}
}
