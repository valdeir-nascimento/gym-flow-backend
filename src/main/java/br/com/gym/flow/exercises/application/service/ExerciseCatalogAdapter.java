package br.com.gym.flow.exercises.application.service;

import br.com.gym.flow.exercises.domain.ExerciseId;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import br.com.gym.flow.exercises.domain.spi.ExerciseCatalog;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implements the {@link ExerciseCatalog} SPI on top of the
 * {@link ExerciseRepository}, projecting the aggregate to {@link ExerciseView}.
 */
@Component
@RequiredArgsConstructor
class ExerciseCatalogAdapter implements ExerciseCatalog {

    private final ExerciseRepository repository;

    @Override
    public Optional<ExerciseView> findById(UUID exerciseId) {
        return repository.findById(ExerciseId.of(exerciseId)).map(ExerciseViewMapper::toView);
    }
}
