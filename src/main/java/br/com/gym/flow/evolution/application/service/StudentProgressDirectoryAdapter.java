package br.com.gym.flow.evolution.application.service;

import br.com.gym.flow.evolution.domain.EvolutionCalculator;
import br.com.gym.flow.evolution.domain.Granularity;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
import br.com.gym.flow.evolution.domain.spi.StudentActivitySummary;
import br.com.gym.flow.evolution.domain.spi.StudentProgressDirectory;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionDirectory;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Implements the {@link StudentProgressDirectory} SPI on top of the
 * {@code history} module's executions, reusing the {@link EvolutionCalculator}.
 */
@Component
@RequiredArgsConstructor
class StudentProgressDirectoryAdapter implements StudentProgressDirectory {

    private final WorkoutExecutionDirectory executions;

    @Override
    public List<StudentActivitySummary> summariesOf(final Collection<UUID> studentIds) {
        return executions.summariesOf(studentIds).stream()
            .map(s -> new StudentActivitySummary(s.studentId(), s.lastExecutionAt(), s.totalExecutions()))
            .toList();
    }

    @Override
    public EvolutionReport reportOf(final UUID studentId, final Instant from, final Instant to) {
        List<WorkoutExecutionView> window = executions.findByStudentInWindow(studentId, from, to);
        return EvolutionCalculator.compute(studentId, Granularity.WEEKLY, from, to, window);
    }
}
