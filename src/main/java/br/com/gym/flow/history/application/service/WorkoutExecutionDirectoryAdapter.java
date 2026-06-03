package br.com.gym.flow.history.application.service;

import br.com.gym.flow.history.domain.WorkoutExecutionRepository;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionDirectory;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implements the {@link WorkoutExecutionDirectory} SPI on top of the
 * {@link WorkoutExecutionRepository}, projecting each aggregate to a
 * {@link WorkoutExecutionView}.
 */
@Component
@RequiredArgsConstructor
class WorkoutExecutionDirectoryAdapter implements WorkoutExecutionDirectory {

    private final WorkoutExecutionRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<WorkoutExecutionView> findByStudentInWindow(final UUID studentId, final Instant from, final Instant to) {
        return repository.findByStudentInWindow(studentId, from, to).stream()
            .map(WorkoutExecutionViewMapper::toView)
            .toList();
    }
}
