package br.com.gym.flow.evolution.presentation;

import br.com.gym.flow.evolution.application.usecase.GetStudentEvolutionQuery;
import br.com.gym.flow.evolution.application.usecase.GetStudentEvolutionUseCase;
import br.com.gym.flow.evolution.domain.Granularity;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Read side of a student's evolution (RF-008). Ownership is enforced by the use
 * case; granularity defaults to weekly and the window (from/to) defaults to the
 * last few months, capped at 24 months. The acting user comes from the
 * {@code X-User-Id} header.
 */
@RestController
@RequestMapping("/evolution")
@RequiredArgsConstructor
class EvolutionController implements EvolutionApi {

    private final GetStudentEvolutionUseCase getEvolution;

    @Override
    @GetMapping
    public Result<EvolutionReport> getEvolution(
        @RequestParam UUID studentId,
        @RequestParam(defaultValue = "WEEKLY") Granularity granularity,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestHeader("X-User-Id") UUID actorId,
        @RequestHeader("X-User-Role") String actorRole
    ) {
        return getEvolution.execute(
            new GetStudentEvolutionQuery(studentId, granularity, from, to, actorId, actorRole));
    }
}
