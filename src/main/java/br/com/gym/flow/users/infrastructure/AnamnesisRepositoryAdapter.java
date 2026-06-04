package br.com.gym.flow.users.infrastructure;

import br.com.gym.flow.users.domain.anamnesis.AnamnesisRepository;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
class AnamnesisRepositoryAdapter implements AnamnesisRepository {

    private final AnamnesisRevisionJpaSpringRepository revisions;
    private final AnamnesisContraindicationJpaSpringRepository contraindications;

    @Override
    public AnamnesisRevision save(final AnamnesisRevision revision) {
        // Immutable revision: inserted once together with its contraindications.
        AnamnesisRevisionJpaEntity saved = revisions.save(AnamnesisJpaMapper.toEntity(revision));
        List<AnamnesisContraindicationJpaEntity> items = AnamnesisJpaMapper.toContraindicationEntities(revision);
        contraindications.saveAll(items);
        return AnamnesisJpaMapper.toDomain(saved, items);
    }

    @Override
    public int latestVersion(final UUID studentId) {
        return revisions.maxVersionByStudent(studentId);
    }

    @Override
    public Optional<AnamnesisRevision> findLatestByStudent(final UUID studentId) {
        return revisions.findFirstByStudentIdOrderByVersionDesc(studentId)
            .map(entity -> AnamnesisJpaMapper.toDomain(
                entity, contraindications.findByAnamnesisIdOrderByPosition(entity.id)));
    }

    @Override
    public List<AnamnesisRevision> findHistoryByStudent(final UUID studentId) {
        List<AnamnesisRevisionJpaEntity> rows = revisions.findByStudentIdOrderByVersionDesc(studentId);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<AnamnesisContraindicationJpaEntity>> byRevision =
            contraindications.findByAnamnesisIdInOrderByAnamnesisIdAscPositionAsc(rows.stream().map(r -> r.id).toList())
                .stream().collect(Collectors.groupingBy(c -> c.anamnesisId));
        return rows.stream()
            .map(entity -> AnamnesisJpaMapper.toDomain(entity, byRevision.getOrDefault(entity.id, List.of())))
            .toList();
    }
}
