package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.application.service.TrainingViewMapper;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import br.com.gym.flow.trainings.domain.spi.TrainingView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListStudentTrainingsUseCase implements QueryUseCase<ListStudentTrainingsQuery, Page<TrainingView>> {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    private final TrainingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Result<Page<TrainingView>> execute(ListStudentTrainingsQuery query) {
        // Ownership (RNF-001): a student only sees their own trainings; an
        // Administrator may inspect anyone. (Instructors use RF-010.)
        if (!ADMINISTRATOR.equals(query.actorRole()) && !query.actorId().equals(query.studentId())) {
            return Result.failWith(ErrorCode.TRAINING_NOT_STUDENT_OWNER);
        }

        Page<TrainingView> page = repository.findByStudentId(query.studentId(), query.pageable())
            .map(TrainingViewMapper::toView);
        return Result.success(page);
    }
}
