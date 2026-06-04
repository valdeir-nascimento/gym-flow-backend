package br.com.gym.flow.coaching.application.usecase;

import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
import br.com.gym.flow.evolution.domain.spi.StudentProgressDirectory;
import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.spi.TeacherStudentDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class GetManagedStudentUseCase implements QueryUseCase<GetManagedStudentQuery, EvolutionReport> {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";
    private static final int WINDOW_MONTHS = 6;

    private final TeacherStudentDirectory bonds;
    private final StudentProgressDirectory progress;
    private final Clock clock;

    @Override
    public Result<EvolutionReport> execute(final GetManagedStudentQuery query) {
        // Authorization (RNF-001, RF-016): the responsible instructor or an
        // Administrator (403 otherwise). Empty indicators are not an error.
        if (!ADMINISTRATOR.equals(query.actorRole())
            && !bonds.hasActiveBond(query.studentId(), query.instructorId())) {
            return Result.failWith(ErrorCode.STUDENT_NOT_MANAGED);
        }

        Instant to = Instant.now(clock);
        Instant from = to.atZone(ZoneOffset.UTC).minusMonths(WINDOW_MONTHS).toInstant();
        return Result.success(progress.reportOf(query.studentId(), from, to));
    }
}
