package br.com.gym.flow.exercises.domain.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * SPI exposed by the {@code exercises} module so other modules (e.g.
 * {@code treinos}) can resolve an exercise by id — to check existence and
 * whether it is still active — without depending on the aggregate.
 */
public interface ExerciseCatalog {

    Optional<ExerciseView> findById(UUID exerciseId);
}
