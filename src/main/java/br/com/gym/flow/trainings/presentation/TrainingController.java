package br.com.gym.flow.trainings.presentation;

import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.application.usecase.CreateTrainingCommand;
import br.com.gym.flow.trainings.application.usecase.CreateTrainingUseCase;
import br.com.gym.flow.trainings.application.usecase.GetTrainingQuery;
import br.com.gym.flow.trainings.application.usecase.GetTrainingUseCase;
import br.com.gym.flow.trainings.application.usecase.ListStudentTrainingsQuery;
import br.com.gym.flow.trainings.application.usecase.ListStudentTrainingsUseCase;
import br.com.gym.flow.trainings.application.usecase.UpdateTrainingCommand;
import br.com.gym.flow.trainings.application.usecase.UpdateTrainingUseCase;
import br.com.gym.flow.trainings.domain.TrainingId;
import br.com.gym.flow.trainings.domain.TrainingItem;
import br.com.gym.flow.trainings.domain.spi.TrainingView;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trainings")
@RequiredArgsConstructor
class TrainingController implements TrainingApi {

    private final CreateTrainingUseCase createTraining;
    private final UpdateTrainingUseCase updateTraining;
    private final ListStudentTrainingsUseCase listStudentTrainings;
    private final GetTrainingUseCase getTraining;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public Result<TrainingView> create(@Valid @RequestBody CreateTrainingRequest req,
                                @RequestHeader("X-User-Id") UUID instructorId,
                                @RequestHeader("X-User-Role") String actorRole,
                                HttpServletResponse response) {
        List<TrainingItem> items = req.items().stream()
            .map(i -> new TrainingItem(i.exerciseId(), i.sets(), i.repetitions(), i.load(), i.restSeconds()))
            .toList();
        Result<TrainingView> result = createTraining.execute(new CreateTrainingCommand(
            req.studentId(), req.name(), req.objective(), req.startDate(), req.endDate(), items, instructorId, actorRole));
        if (result.isSuccess()) {
            response.setHeader(HttpHeaders.LOCATION, "/trainings/" + result.getOrThrow().id());
        }
        return result;
    }

    @PutMapping("/{id}")
    @Override
    public Result<TrainingView> update(@PathVariable UUID id,
                                @Valid @RequestBody UpdateTrainingRequest req,
                                @RequestHeader("X-User-Id") UUID actorId,
                                @RequestHeader("X-User-Role") String actorRole) {
        List<TrainingItem> items = req.items().stream()
            .map(i -> new TrainingItem(i.exerciseId(), i.sets(), i.repetitions(), i.load(), i.restSeconds()))
            .toList();
        return updateTraining.execute(new UpdateTrainingCommand(
            TrainingId.of(id), req.name(), req.objective(), req.startDate(), req.endDate(),
            items, req.status(), actorId, actorRole));
    }

    @GetMapping
    @Override
    public Result<Page<TrainingView>> listByStudent(@RequestParam UUID studentId,
                                             @RequestHeader("X-User-Id") UUID actorId,
                                             @RequestHeader("X-User-Role") String actorRole,
                                             Pageable pageable) {
        return listStudentTrainings.execute(
            new ListStudentTrainingsQuery(studentId, actorId, actorRole, pageable));
    }

    @GetMapping("/{id}")
    @Override
    public Result<TrainingView> getById(@PathVariable UUID id) {
        return getTraining.execute(new GetTrainingQuery(TrainingId.of(id)));
    }
}
