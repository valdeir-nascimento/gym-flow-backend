package br.com.gym.flow.users.infrastructure;

import br.com.gym.flow.users.domain.consent.HealthDataConsent;
import br.com.gym.flow.users.domain.consent.HealthDataConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class HealthDataConsentRepositoryAdapter implements HealthDataConsentRepository {

    private final HealthDataConsentJpaSpringRepository jpa;

    @Override
    public HealthDataConsent save(final HealthDataConsent consent) {
        HealthDataConsentJpaEntity entity = new HealthDataConsentJpaEntity();
        entity.id = consent.id();
        entity.studentId = consent.studentId();
        entity.grantedBy = consent.grantedBy();
        entity.grantedAt = consent.grantedAt();
        return toDomain(jpa.save(entity));
    }

    @Override
    public boolean existsByStudentId(final UUID studentId) {
        return jpa.existsByStudentId(studentId);
    }

    @Override
    public Optional<HealthDataConsent> findByStudentId(final UUID studentId) {
        return jpa.findByStudentId(studentId).map(HealthDataConsentRepositoryAdapter::toDomain);
    }

    private static HealthDataConsent toDomain(final HealthDataConsentJpaEntity entity) {
        return new HealthDataConsent(entity.id, entity.studentId, entity.grantedBy, entity.grantedAt);
    }
}
