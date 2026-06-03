package br.com.gym.flow.users.infrastructure;

import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.bond.BondId;
import br.com.gym.flow.users.domain.bond.TeacherStudentBond;

final class BondJpaMapper {
    private BondJpaMapper() {}

    static BondJpaEntity toEntity(TeacherStudentBond bond, BondJpaEntity existing) {
        BondJpaEntity entity = existing == null ? new BondJpaEntity() : existing;
        entity.id = bond.id().value();
        entity.studentId = bond.studentId().value();
        entity.instructorId = bond.instructorId().value();
        entity.startedAt = bond.startedAt();
        entity.endedAt = bond.endedAt();
        entity.createdBy = bond.createdBy().value();
        if (entity.createdAt == null) {
            entity.createdAt = bond.startedAt();
        }
        return entity;
    }

    static TeacherStudentBond toDomain(BondJpaEntity entity) {
        return TeacherStudentBond.hydrate(
            BondId.of(entity.id),
            UserId.of(entity.studentId),
            UserId.of(entity.instructorId),
            entity.startedAt,
            entity.endedAt,
            UserId.of(entity.createdBy)
        );
    }
}
