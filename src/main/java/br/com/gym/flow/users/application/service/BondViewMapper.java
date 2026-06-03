package br.com.gym.flow.users.application.service;

import br.com.gym.flow.users.domain.bond.TeacherStudentBond;
import br.com.gym.flow.users.domain.spi.BondView;

public final class BondViewMapper {
    private BondViewMapper() {}

    public static BondView toView(TeacherStudentBond bond) {
        return new BondView(
            bond.id().value(),
            bond.studentId().value(),
            bond.instructorId().value(),
            bond.startedAt(),
            bond.endedAt(),
            bond.createdBy().value(),
            bond.isActive()
        );
    }
}
