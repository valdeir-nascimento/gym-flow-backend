package br.com.gym.flow.users.domain.spi;

import java.util.List;
import java.util.UUID;

/**
 * SPI exposed by the {@code users} module so other modules (e.g. {@code trainings})
 * can read anamnesis-derived facts about a student without depending on the
 * aggregate — namely the exercises contraindicated for the student (RF-017),
 * which must not compose their trainings (RF-004/RF-005).
 */
public interface AnamnesisDirectory {

    /**
     * Exercise ids contraindicated for the student per their latest anamnesis
     * revision; empty when the student has no anamnesis.
     */
    List<UUID> contraindicatedExercises(UUID studentId);
}
