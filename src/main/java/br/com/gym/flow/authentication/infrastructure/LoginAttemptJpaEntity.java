package br.com.gym.flow.authentication.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "login_attempts")
class LoginAttemptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "email", nullable = false, length = 255)
    String email;

    @Column(name = "attempted_at", nullable = false)
    Instant attemptedAt;

    @Column(name = "success", nullable = false)
    boolean success;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    LoginAttemptJpaEntity() {}
}
