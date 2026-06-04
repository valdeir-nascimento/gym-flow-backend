package br.com.gym.flow.users.domain.consent;

import java.util.Optional;
import java.util.UUID;

public interface HealthDataConsentRepository {

    HealthDataConsent save(HealthDataConsent consent);

    boolean existsByStudentId(UUID studentId);

    Optional<HealthDataConsent> findByStudentId(UUID studentId);
}
