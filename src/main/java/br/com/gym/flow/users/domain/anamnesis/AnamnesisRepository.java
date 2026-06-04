package br.com.gym.flow.users.domain.anamnesis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnamnesisRepository {

    AnamnesisRevision save(AnamnesisRevision revision);

    /** Highest revision version registered for the student, or 0 when none (RF-017). */
    int latestVersion(UUID studentId);

    /** The current (highest-version) revision of the student (RF-017 ConsultarAnamnese). */
    Optional<AnamnesisRevision> findLatestByStudent(UUID studentId);

    /** All revisions of the student, most recent first (RF-017 ConsultarHistoricoAnamneses). */
    List<AnamnesisRevision> findHistoryByStudent(UUID studentId);
}
