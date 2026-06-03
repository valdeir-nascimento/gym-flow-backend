package br.com.gym.flow.users.domain.bond;

import br.com.gym.flow.users.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BondRepository {

    TeacherStudentBond save(TeacherStudentBond bond);

    Optional<TeacherStudentBond> findById(BondId id);

    Optional<TeacherStudentBond> findActiveByStudent(UserId studentId);

    Page<TeacherStudentBond> findByInstructor(UserId instructorId, Pageable pageable);

    Page<TeacherStudentBond> findByStudent(UserId studentId, Pageable pageable);
}
