package br.com.gym.flow.authentication.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
class UserCredentialsJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "email", nullable = false, length = 255)
    String email;

    @Column(name = "password_hash", length = 72)
    String passwordHash;

    @Column(name = "password_updated_at")
    Instant passwordUpdatedAt;

    @Column(name = "role", nullable = false, length = 20)
    String role;

    @Column(name = "status", nullable = false, length = 30)
    String status;

    UserCredentialsJpaEntity() {}
}
