package br.com.gym.flow.trainings.domain.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * SPI exposed by the {@code trainings} module so other modules (e.g.
 * {@code history}) can resolve a training by id — to check ownership and
 * whether it was active at a point in time — without depending on the aggregate.
 */
public interface TrainingDirectory {

    Optional<TrainingDigest> findById(UUID trainingId);
}
