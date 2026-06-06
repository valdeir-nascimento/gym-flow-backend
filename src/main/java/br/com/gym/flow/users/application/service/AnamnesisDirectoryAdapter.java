package br.com.gym.flow.users.application.service;

import br.com.gym.flow.users.domain.anamnesis.AnamnesisRepository;
import br.com.gym.flow.users.domain.spi.AnamnesisDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implements the {@link AnamnesisDirectory} SPI on top of the
 * {@link AnamnesisRepository}, projecting the latest revision's contraindicated
 * exercises (empty when the student has no anamnesis).
 */
@Component
@RequiredArgsConstructor
class AnamnesisDirectoryAdapter implements AnamnesisDirectory {

    private final AnamnesisRepository anamneses;

    @Override
    @Transactional(readOnly = true)
    public List<UUID> contraindicatedExercises(final UUID studentId) {
        return anamneses.findLatestByStudent(studentId)
            .map(revision -> revision.contraindications())
            .orElseGet(List::of);
    }
}
