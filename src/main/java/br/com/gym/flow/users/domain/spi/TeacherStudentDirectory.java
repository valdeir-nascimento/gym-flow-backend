package br.com.gym.flow.users.domain.spi;

import java.util.UUID;

/**
 * SPI exposed by the {@code users} module so other modules can check the
 * student↔instructor relationship (RF-016) for authorization — e.g.
 * {@code treinos} verifying that a professor may prescribe to a student.
 */
public interface TeacherStudentDirectory {

    boolean hasActiveBond(UUID studentId, UUID instructorId);
}
