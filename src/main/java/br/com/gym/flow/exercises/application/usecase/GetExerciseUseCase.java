package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.application.service.ExerciseViewMapper;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetExerciseUseCase implements QueryUseCase<GetExerciseQuery, ExerciseView> {

    private final ExerciseRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Result<ExerciseView> execute(GetExerciseQuery query) {
        return Result.ofOptional(repository.findById(query.exerciseId()).map(ExerciseViewMapper::toView), ErrorCode.EXERCISE_NOT_FOUND);
    }
}
