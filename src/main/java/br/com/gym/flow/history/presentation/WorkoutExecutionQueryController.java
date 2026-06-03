package br.com.gym.flow.history.presentation;

import br.com.gym.flow.history.application.usecase.GetWorkoutExecutionQuery;
import br.com.gym.flow.history.application.usecase.GetWorkoutExecutionUseCase;
import br.com.gym.flow.history.application.usecase.ListWorkoutExecutionsQuery;
import br.com.gym.flow.history.application.usecase.ListWorkoutExecutionsUseCase;
import br.com.gym.flow.history.domain.WorkoutExecutionId;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.domain.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Read side of the workout-execution resource (RF-009). The history is always
 * ordered most-recent-first, so the client controls page/size (size capped at
 * 100) but not sort. The acting student comes from the {@code X-User-Id} header.
 */
@RestController
@RequestMapping("/workout-executions")
@RequiredArgsConstructor
class WorkoutExecutionQueryController {

    private final ListWorkoutExecutionsUseCase listExecutions;
    private final GetWorkoutExecutionUseCase getExecution;

    @GetMapping
    Result<Page<WorkoutExecutionView>> list(
        @RequestParam UUID studentId,
        @RequestParam(required = false) UUID trainingId,
        @RequestParam(required = false) UUID exerciseId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestHeader("X-User-Id") UUID actorId,
        @RequestHeader("X-User-Role") String actorRole
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        return listExecutions.execute(new ListWorkoutExecutionsQuery(
            studentId, trainingId, exerciseId, startedFrom, startedTo, actorId, actorRole, pageable));
    }

    @GetMapping("/{id}")
    Result<WorkoutExecutionView> getById(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") UUID actorId,
        @RequestHeader("X-User-Role") String actorRole
    ) {
        return getExecution.execute(new GetWorkoutExecutionQuery(WorkoutExecutionId.of(id), actorId, actorRole));
    }
}
