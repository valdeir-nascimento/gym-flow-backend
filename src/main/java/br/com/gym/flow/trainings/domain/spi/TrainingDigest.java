package br.com.gym.flow.trainings.domain.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-module projection of a {@code Training} (RF-007). Exposes only what
 * consumers such as the {@code history} module need to authorize and
 * time-bound a workout execution: the owning student, the current status and,
 * when archived, the moment the training became inactive.
 */
public record TrainingDigest(
    UUID trainingId,
    UUID studentId,
    String status,
    Instant inactivatedAt
) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
