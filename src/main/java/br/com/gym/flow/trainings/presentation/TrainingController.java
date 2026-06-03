package br.com.gym.flow.trainings.presentation;

import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.application.usecase.CreateTrainingCommand;
import br.com.gym.flow.trainings.application.usecase.CreateTrainingUseCase;
import br.com.gym.flow.trainings.application.usecase.GetTrainingQuery;
import br.com.gym.flow.trainings.application.usecase.GetTrainingUseCase;
import br.com.gym.flow.trainings.domain.TrainingId;
import br.com.gym.flow.trainings.domain.TrainingItem;
import br.com.gym.flow.trainings.domain.spi.TrainingView;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/trainings")
@RequiredArgsConstructor
class TrainingController {

    private final CreateTrainingUseCase createTraining;
    private final GetTrainingUseCase getTraining;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Result<TrainingView> create(@Valid @RequestBody CreateTrainingRequest req,
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

    @GetMapping("/{id}")
    Result<TrainingView> getById(@PathVariable UUID id) {
        return getTraining.execute(new GetTrainingQuery(TrainingId.of(id)));
    }
}
