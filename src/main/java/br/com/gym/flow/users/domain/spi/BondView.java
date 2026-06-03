package br.com.gym.flow.users.domain.spi;

import java.time.Instant;
import java.util.UUID;

public record BondView(
    UUID id,
    UUID studentId,
    UUID instructorId,
    Instant startedAt,
    Instant endedAt,
    UUID createdBy,
    boolean active
) {}
