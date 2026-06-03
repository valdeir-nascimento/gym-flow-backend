package br.com.gym.flow.history.application.usecase;

import br.com.gym.flow.history.application.service.WorkoutExecutionViewMapper;
import br.com.gym.flow.history.domain.WorkoutExecutionFilter;
import br.com.gym.flow.history.domain.WorkoutExecutionRepository;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListWorkoutExecutionsUseCase
    implements QueryUseCase<ListWorkoutExecutionsQuery, Page<WorkoutExecutionView>> {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    private final WorkoutExecutionRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Result<Page<WorkoutExecutionView>> execute(final ListWorkoutExecutionsQuery query) {
        // Ownership (RNF-001): a student only sees their own history; an
        // Administrator may inspect anyone.
        if (!ADMINISTRATOR.equals(query.actorRole()) && !query.actorId().equals(query.studentId())) {
            return Result.failWith(ErrorCode.EXECUTION_NOT_OWNED);
        }

        WorkoutExecutionFilter filter = new WorkoutExecutionFilter(
            query.studentId(), query.trainingId(), query.exerciseId(), query.startedFrom(), query.startedTo());
        Page<WorkoutExecutionView> page = repository.search(filter, query.pageable())
            .map(WorkoutExecutionViewMapper::toView);
        return Result.success(page);
    }
}
