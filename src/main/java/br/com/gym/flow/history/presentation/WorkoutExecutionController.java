package br.com.gym.flow.history.presentation;

import br.com.gym.flow.history.application.usecase.RegisterWorkoutExecutionCommand;
import br.com.gym.flow.history.application.usecase.RegisterWorkoutExecutionUseCase;
import br.com.gym.flow.history.domain.ExecutedExercise;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.domain.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trainings/{trainingId}/executions")
@RequiredArgsConstructor
class WorkoutExecutionController {

    private final RegisterWorkoutExecutionUseCase registerExecution;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Result<WorkoutExecutionView> register(
        @PathVariable UUID trainingId,
        @Valid @RequestBody RegisterWorkoutExecutionRequest req,
        @RequestHeader("X-User-Id") UUID actorId,
        HttpServletResponse response
    ) {
        List<ExecutedExercise> items = req.items().stream()
            .map(i -> new ExecutedExercise(i.exerciseId(), i.sets(), i.repetitions(), i.load(), i.notes()))
            .toList();
        Result<WorkoutExecutionView> result = registerExecution.execute(new RegisterWorkoutExecutionCommand(
            trainingId, actorId, req.startedAt(), req.finishedAt(), req.notes(), items));
        if (result.isSuccess()) {
            response.setHeader(HttpHeaders.LOCATION,
                "/trainings/" + trainingId + "/executions/" + result.getOrThrow().id());
        }
        return result;
    }
}
