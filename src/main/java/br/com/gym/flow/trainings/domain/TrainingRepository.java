package br.com.gym.flow.trainings.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface TrainingRepository {

    Training save(Training training);

    Optional<Training> findById(TrainingId id);

    /** Trainings owned by the student (RF-006), most recent first, paginated. */
    Page<Training> findByStudentId(UUID studentId, Pageable pageable);

    /**
     * Whether the student already has an ACTIVE training whose validity window
     * overlaps {@code period} (RF-004: overlapping active trainings are blocked).
     */
    boolean existsActiveOverlapping(UUID studentId, TrainingPeriod period);
}
