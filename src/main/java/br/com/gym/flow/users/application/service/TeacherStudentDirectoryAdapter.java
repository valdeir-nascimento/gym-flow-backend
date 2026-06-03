package br.com.gym.flow.users.application.service;

import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.spi.TeacherStudentDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implements the {@link TeacherStudentDirectory} SPI: a student has an active
 * bond with the given instructor when their single active bond points to him.
 */
@Component
@RequiredArgsConstructor
class TeacherStudentDirectoryAdapter implements TeacherStudentDirectory {

    private final BondRepository bonds;

    @Override
    public boolean hasActiveBond(UUID studentId, UUID instructorId) {
        return bonds.findActiveByStudent(UserId.of(studentId))
            .map(bond -> bond.instructorId().equals(UserId.of(instructorId)))
            .orElse(false);
    }
}
