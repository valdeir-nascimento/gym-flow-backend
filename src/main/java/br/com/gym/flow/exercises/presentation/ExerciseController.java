package br.com.gym.flow.exercises.presentation;

import br.com.gym.flow.exercises.application.usecase.DeactivateExerciseCommand;
import br.com.gym.flow.exercises.application.usecase.DeactivateExerciseUseCase;
import br.com.gym.flow.exercises.application.usecase.GetExerciseQuery;
import br.com.gym.flow.exercises.application.usecase.GetExerciseUseCase;
import br.com.gym.flow.exercises.application.usecase.ListExercisesQuery;
import br.com.gym.flow.exercises.application.usecase.ListExercisesUseCase;
import br.com.gym.flow.exercises.application.usecase.RegisterExerciseCommand;
import br.com.gym.flow.exercises.application.usecase.RegisterExerciseUseCase;
import br.com.gym.flow.exercises.application.usecase.UpdateExerciseCommand;
import br.com.gym.flow.exercises.application.usecase.UpdateExerciseUseCase;
import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.ExerciseId;
import br.com.gym.flow.exercises.domain.ExerciseStatus;
import br.com.gym.flow.exercises.domain.MuscleGroup;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.domain.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/exercises")
@RequiredArgsConstructor
class ExerciseController implements ExerciseApi {

    private final RegisterExerciseUseCase registerExercise;
    private final UpdateExerciseUseCase updateExercise;
    private final DeactivateExerciseUseCase deactivateExercise;
    private final ListExercisesUseCase listExercises;
    private final GetExerciseUseCase getExercise;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public Result<ExerciseView> register(@Valid @RequestBody RegisterExerciseRequest req,
                                  @RequestHeader("X-User-Id") UUID actorId,
                                  @RequestHeader("X-User-Role") String actorRole,
                                  HttpServletResponse response) {
        Result<ExerciseView> result = registerExercise.execute(new RegisterExerciseCommand(
            req.name(), req.muscleGroup(), req.description(), req.equipment(),
            req.difficultyLevel(), req.videoUrl(), req.imageUrl(), actorId, actorRole));
        if (result.isSuccess()) {
            response.setHeader(HttpHeaders.LOCATION, "/exercises/" + result.getOrThrow().id());
        }
        return result;
    }

    @PutMapping("/{id}")
    @Override
    public Result<ExerciseView> update(@PathVariable UUID id,
                                @Valid @RequestBody UpdateExerciseRequest req,
                                @RequestHeader("X-User-Role") String actorRole) {
        return updateExercise.execute(new UpdateExerciseCommand(
            ExerciseId.of(id), req.name(), req.muscleGroup(), req.description(), req.equipment(),
            req.difficultyLevel(), req.videoUrl(), req.imageUrl(), actorRole));
    }

    @PatchMapping("/{id}/deactivate")
    @Override
    public Result<ExerciseView> deactivate(@PathVariable UUID id,
                                    @RequestHeader("X-User-Role") String actorRole) {
        return deactivateExercise.execute(new DeactivateExerciseCommand(ExerciseId.of(id), actorRole));
    }

    @GetMapping
    @Override
    public Result<Page<ExerciseView>> list(@RequestParam(required = false) MuscleGroup muscleGroup,
                                    @RequestParam(required = false) DifficultyLevel difficultyLevel,
                                    @RequestParam(required = false) String equipment,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) ExerciseStatus status,
                                    Pageable pageable) {
        return listExercises.execute(new ListExercisesQuery(
            muscleGroup, difficultyLevel, equipment, search, status, pageable));
    }

    @GetMapping("/{id}")
    @Override
    public Result<ExerciseView> getById(@PathVariable UUID id) {
        return getExercise.execute(new GetExerciseQuery(ExerciseId.of(id)));
    }
}
