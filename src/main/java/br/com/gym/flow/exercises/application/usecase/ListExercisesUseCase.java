package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.application.service.ExerciseViewMapper;
import br.com.gym.flow.exercises.domain.ExerciseFilter;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListExercisesUseCase implements QueryUseCase<ListExercisesQuery, Page<ExerciseView>> {

    private final ExerciseRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Result<Page<ExerciseView>> execute(ListExercisesQuery query) {
        ExerciseFilter filter = new ExerciseFilter(
            query.muscleGroup(), query.difficultyLevel(), query.equipment(), query.search(), query.status());
        Page<ExerciseView> page = repository.search(filter, query.pageable()).map(ExerciseViewMapper::toView);
        return Result.success(page);
    }
}
