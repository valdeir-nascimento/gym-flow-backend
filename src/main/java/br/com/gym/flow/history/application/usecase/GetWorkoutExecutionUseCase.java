package br.com.gym.flow.history.application.usecase;

import br.com.gym.flow.history.application.service.WorkoutExecutionViewMapper;
import br.com.gym.flow.history.domain.WorkoutExecution;
import br.com.gym.flow.history.domain.WorkoutExecutionRepository;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetWorkoutExecutionUseCase implements QueryUseCase<GetWorkoutExecutionQuery, WorkoutExecutionView> {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    private final WorkoutExecutionRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Result<WorkoutExecutionView> execute(final GetWorkoutExecutionQuery query) {
        Optional<WorkoutExecution> maybe = repository.findById(query.executionId());
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.EXECUTION_NOT_FOUND);
        }
        WorkoutExecution execution = maybe.get();

        // Ownership (RNF-001): only the owning student (or an Administrator) may
        // read the detail (403).
        if (!ADMINISTRATOR.equals(query.actorRole()) && !execution.studentId().equals(query.actorId())) {
            return Result.failWith(ErrorCode.EXECUTION_NOT_OWNED);
        }

        return Result.success(WorkoutExecutionViewMapper.toView(execution));
    }
}
