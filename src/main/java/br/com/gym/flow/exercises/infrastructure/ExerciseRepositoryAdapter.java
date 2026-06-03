package br.com.gym.flow.exercises.infrastructure;

import br.com.gym.flow.exercises.domain.Exercise;
import br.com.gym.flow.exercises.domain.ExerciseFilter;
import br.com.gym.flow.exercises.domain.ExerciseId;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ExerciseRepositoryAdapter implements ExerciseRepository {

    private final ExerciseJpaSpringRepository jpa;

    @Override
    public Exercise save(Exercise exercise) {
        ExerciseJpaEntity existing = jpa.findById(exercise.id().value()).orElse(null);
        ExerciseJpaEntity entity = ExerciseJpaMapper.toEntity(exercise, existing);
        return ExerciseJpaMapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Exercise> findById(ExerciseId id) {
        return jpa.findById(id.value()).map(ExerciseJpaMapper::toDomain);
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return jpa.existsByNameIgnoreCase(name);
    }

    @Override
    public Page<Exercise> search(ExerciseFilter filter, Pageable pageable) {
        String muscleGroup = filter.muscleGroup() == null ? null : filter.muscleGroup().name();
        String difficultyLevel = filter.difficultyLevel() == null ? null : filter.difficultyLevel().name();
        String status = filter.status() == null ? null : filter.status().name();
        String equipment = blankToNull(filter.equipment());
        String search = blankToNull(filter.search());
        return jpa.search(muscleGroup, difficultyLevel, equipment, search, status, pageable)
            .map(ExerciseJpaMapper::toDomain);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
