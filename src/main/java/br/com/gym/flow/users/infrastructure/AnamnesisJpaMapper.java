package br.com.gym.flow.users.infrastructure;

import br.com.gym.flow.users.domain.anamnesis.AnamnesisId;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisRevision;
import br.com.gym.flow.users.domain.anamnesis.Height;
import br.com.gym.flow.users.domain.anamnesis.Weight;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

final class AnamnesisJpaMapper {

    private AnamnesisJpaMapper() {
    }

    static AnamnesisRevisionJpaEntity toEntity(final AnamnesisRevision revision) {
        AnamnesisRevisionJpaEntity entity = new AnamnesisRevisionJpaEntity();
        entity.id = revision.id().value();
        entity.studentId = revision.studentId();
        entity.version = revision.version();
        entity.weightKg = revision.weight().kilograms();
        entity.heightCm = revision.height().centimeters();
        entity.objectives = revision.objectives();
        entity.conditioningHistory = revision.conditioningHistory();
        entity.injuries = joinLines(revision.injuries());
        entity.medicalRestrictions = joinLines(revision.medicalRestrictions());
        entity.observations = revision.observations();
        entity.createdBy = revision.createdBy();
        entity.createdAt = revision.createdAt();
        return entity;
    }

    static List<AnamnesisContraindicationJpaEntity> toContraindicationEntities(final AnamnesisRevision revision) {
        List<UUID> exercises = revision.contraindications();
        List<AnamnesisContraindicationJpaEntity> entities = new ArrayList<>(exercises.size());
        for (int position = 0; position < exercises.size(); position++) {
            AnamnesisContraindicationJpaEntity entity = new AnamnesisContraindicationJpaEntity();
            entity.id = UUID.randomUUID();
            entity.anamnesisId = revision.id().value();
            entity.exerciseId = exercises.get(position);
            entity.position = position;
            entities.add(entity);
        }
        return entities;
    }

    static AnamnesisRevision toDomain(
        final AnamnesisRevisionJpaEntity entity,
        final List<AnamnesisContraindicationJpaEntity> contraindications
    ) {
        List<UUID> exerciseIds = contraindications.stream()
            .map(c -> c.exerciseId)
            .toList();
        return AnamnesisRevision.hydrate(
            AnamnesisId.of(entity.id),
            entity.studentId,
            entity.version,
            Weight.of(entity.weightKg),
            Height.of(entity.heightCm),
            entity.objectives,
            entity.conditioningHistory,
            splitLines(entity.injuries),
            splitLines(entity.medicalRestrictions),
            exerciseIds,
            entity.observations,
            entity.createdBy,
            entity.createdAt);
    }

    private static String joinLines(final List<String> values) {
        return values == null || values.isEmpty() ? null : String.join("\n", values);
    }

    private static List<String> splitLines(final String joined) {
        return joined == null || joined.isBlank() ? List.of() : Arrays.asList(joined.split("\n"));
    }
}
