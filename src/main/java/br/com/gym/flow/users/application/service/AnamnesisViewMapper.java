package br.com.gym.flow.users.application.service;

import br.com.gym.flow.users.domain.anamnesis.AnamnesisRevision;
import br.com.gym.flow.users.domain.consent.HealthDataConsent;
import br.com.gym.flow.users.domain.spi.AnamnesisView;
import br.com.gym.flow.users.domain.spi.HealthConsentView;

public final class AnamnesisViewMapper {

    private AnamnesisViewMapper() {
    }

    public static AnamnesisView toView(final AnamnesisRevision revision) {
        return new AnamnesisView(
            revision.id().value(),
            revision.studentId(),
            revision.version(),
            revision.weight().kilograms(),
            revision.height().centimeters(),
            revision.objectives(),
            revision.conditioningHistory(),
            revision.injuries(),
            revision.medicalRestrictions(),
            revision.contraindications(),
            revision.observations(),
            revision.createdBy(),
            revision.createdAt());
    }

    public static HealthConsentView toView(final HealthDataConsent consent) {
        return new HealthConsentView(consent.id(), consent.studentId(), consent.grantedBy(), consent.grantedAt());
    }
}
