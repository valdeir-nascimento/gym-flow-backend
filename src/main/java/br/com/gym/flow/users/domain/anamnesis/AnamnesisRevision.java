package br.com.gym.flow.users.domain.anamnesis;

import br.com.gym.flow.shared.domain.AggregateRoot;
import br.com.gym.flow.users.events.AnamnesisRegistered;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root of a single, immutable anamnesis revision (RF-017). Each
 * registration creates a new revision for the student, identified by an
 * incrementing {@code version}; older revisions are preserved for history and
 * evolution. References the student and the prescribing instructor by id.
 */
@Getter
@Accessors(fluent = true)
public class AnamnesisRevision extends AggregateRoot<AnamnesisId> {

    private final UUID studentId;
    private final int version;
    private final Weight weight;
    private final Height height;
    private final String objectives;
    private final String conditioningHistory;
    private final List<String> injuries;
    private final List<String> medicalRestrictions;
    private final List<UUID> contraindications;
    private final String observations;
    private final UUID createdBy;
    private final Instant createdAt;

    private AnamnesisRevision(
        final AnamnesisId id,
        final UUID studentId,
        final int version,
        final Weight weight,
        final Height height,
        final String objectives,
        final String conditioningHistory,
        final List<String> injuries,
        final List<String> medicalRestrictions,
        final List<UUID> contraindications,
        final String observations,
        final UUID createdBy,
        final Instant createdAt
    ) {
        super(id);
        this.studentId = studentId;
        this.version = version;
        this.weight = weight;
        this.height = height;
        this.objectives = objectives;
        this.conditioningHistory = conditioningHistory;
        this.injuries = List.copyOf(injuries);
        this.medicalRestrictions = List.copyOf(medicalRestrictions);
        this.contraindications = List.copyOf(contraindications);
        this.observations = observations;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static AnamnesisRevision register(
        final UUID studentId,
        final int version,
        final AnamnesisDraft draft,
        final UUID createdBy,
        final Clock clock
    ) {
        Instant now = Instant.now(clock);
        AnamnesisRevision revision = new AnamnesisRevision(
            AnamnesisId.newId(),
            studentId,
            version,
            draft.weight(),
            draft.height(),
            draft.objectives(),
            draft.conditioningHistory(),
            draft.injuries(),
            draft.medicalRestrictions(),
            draft.contraindications(),
            draft.observations(),
            createdBy,
            now
        );
        revision.registerEvent(AnamnesisRegistered.of(revision.id().value(), studentId, version, createdBy, now));
        return revision;
    }

    public static AnamnesisRevision hydrate(
        final AnamnesisId id,
        final UUID studentId,
        final int version,
        final Weight weight,
        final Height height,
        final String objectives,
        final String conditioningHistory,
        final List<String> injuries,
        final List<String> medicalRestrictions,
        final List<UUID> contraindications,
        final String observations,
        final UUID createdBy,
        final Instant createdAt
    ) {
        return new AnamnesisRevision(id, studentId, version, weight, height, objectives, conditioningHistory,
            injuries, medicalRestrictions, contraindications, observations, createdBy, createdAt);
    }
}
