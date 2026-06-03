package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.application.service.TrainingViewMapper;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import br.com.gym.flow.trainings.domain.spi.TrainingView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTrainingUseCase implements QueryUseCase<GetTrainingQuery, TrainingView> {

    private final TrainingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Result<TrainingView> execute(GetTrainingQuery query) {
        return Result.ofOptional(
            repository.findById(query.trainingId()).map(TrainingViewMapper::toView),
            ErrorCode.TRAINING_NOT_FOUND);
    }
}
