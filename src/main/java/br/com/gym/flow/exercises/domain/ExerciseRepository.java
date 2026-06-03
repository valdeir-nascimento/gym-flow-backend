package br.com.gym.flow.exercises.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ExerciseRepository {

    Exercise save(Exercise exercise);

    Optional<Exercise> findById(ExerciseId id);

    boolean existsByNameIgnoreCase(String name);

    Page<Exercise> search(ExerciseFilter filter, Pageable pageable);
}
