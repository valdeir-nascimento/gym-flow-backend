package br.com.gym.flow.trainings.domain;

import java.util.Optional;
import java.util.UUID;

public interface TrainingRepository {

    Training save(Training training);

    Optional<Training> findById(TrainingId id);

    /**
     * Whether the student already has an ACTIVE training whose validity window
     * overlaps {@code period} (RF-004: overlapping active trainings are blocked).
     */
    boolean existsActiveOverlapping(UUID studentId, TrainingPeriod period);
}
