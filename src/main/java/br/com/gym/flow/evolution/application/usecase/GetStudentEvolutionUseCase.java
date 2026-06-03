package br.com.gym.flow.evolution.application.usecase;

import br.com.gym.flow.evolution.domain.EvolutionCalculator;
import br.com.gym.flow.evolution.domain.Granularity;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionDirectory;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetStudentEvolutionUseCase implements QueryUseCase<GetStudentEvolutionQuery, EvolutionReport> {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";
    private static final int MAX_WINDOW_MONTHS = 24;
    private static final int DEFAULT_WINDOW_MONTHS = 6;

    private final WorkoutExecutionDirectory executions;
    private final Clock clock;

    @Override
    public Result<EvolutionReport> execute(final GetStudentEvolutionQuery query) {
        // Ownership (RNF-001): a student only sees their own evolution; an
        // Administrator may inspect anyone (403 otherwise).
        if (!ADMINISTRATOR.equals(query.actorRole()) && !query.actorId().equals(query.studentId())) {
            return Result.failWith(ErrorCode.EVOLUTION_NOT_OWNED);
        }

        Granularity granularity = query.granularity() != null ? query.granularity() : Granularity.WEEKLY;
        Instant to = query.to() != null ? query.to() : Instant.now(clock);
        Instant from = query.from() != null ? query.from() : minusMonths(to, DEFAULT_WINDOW_MONTHS);

        // The window may span at most 24 months (422).
        if (from.isBefore(minusMonths(to, MAX_WINDOW_MONTHS))) {
            return Result.failWith(ErrorCode.EVOLUTION_PERIOD_TOO_LONG);
        }

        List<WorkoutExecutionView> window = executions.findByStudentInWindow(query.studentId(), from, to);
        // Empty window -> a report with empty series (no error).
        return Result.success(EvolutionCalculator.compute(query.studentId(), granularity, from, to, window));
    }

    private static Instant minusMonths(final Instant reference, final int months) {
        return reference.atZone(ZoneOffset.UTC).minusMonths(months).toInstant();
    }
}
