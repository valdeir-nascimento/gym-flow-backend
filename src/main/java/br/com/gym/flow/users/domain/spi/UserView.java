package br.com.gym.flow.users.domain.spi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserView(
    UUID id,
    String name,
    String email,
    String phone,
    LocalDate birthDate,
    String role,
    String status,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
