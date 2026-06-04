package br.com.gym.flow.users.domain.bond;

import br.com.gym.flow.users.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BondRepository {

    TeacherStudentBond save(TeacherStudentBond bond);

    Optional<TeacherStudentBond> findById(BondId id);

    Optional<TeacherStudentBond> findActiveByStudent(UserId studentId);

    /** Active bonds (not yet ended) of the instructor (RF-010). */
    List<TeacherStudentBond> findActiveByInstructor(UserId instructorId);

    Page<TeacherStudentBond> findByInstructor(UserId instructorId, Pageable pageable);

    Page<TeacherStudentBond> findByStudent(UserId studentId, Pageable pageable);
}
