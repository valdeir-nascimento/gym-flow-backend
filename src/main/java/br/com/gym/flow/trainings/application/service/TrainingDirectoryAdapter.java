package br.com.gym.flow.trainings.application.service;

import br.com.gym.flow.trainings.domain.Training;
import br.com.gym.flow.trainings.domain.TrainingId;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import br.com.gym.flow.trainings.domain.TrainingStatus;
import br.com.gym.flow.trainings.domain.spi.TrainingDigest;
import br.com.gym.flow.trainings.domain.spi.TrainingDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the {@link TrainingDirectory} SPI on top of the
 * {@link TrainingRepository}, projecting the aggregate to a {@link TrainingDigest}.
 * A training has no dedicated inactivation timestamp, so by convention its
 * {@code updatedAt} is taken as the inactivation moment once status is ARCHIVED.
 */
@Component
@RequiredArgsConstructor
class TrainingDirectoryAdapter implements TrainingDirectory {

    private final TrainingRepository repository;

    @Override
    public Optional<TrainingDigest> findById(UUID trainingId) {
        return repository.findById(TrainingId.of(trainingId)).map(this::toDigest);
    }

    private TrainingDigest toDigest(final Training training) {
        Instant inactivatedAt = training.status() == TrainingStatus.ARCHIVED ? training.updatedAt() : null;
        return new TrainingDigest(
            training.id().value(),
            training.studentId(),
            training.status().name(),
            inactivatedAt
        );
    }
}
