package br.com.gym.flow.users.infrastructure;

import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.bond.BondId;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.bond.TeacherStudentBond;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class BondRepositoryAdapter implements BondRepository {

    private final BondJpaSpringRepository jpa;

    @Override
    public TeacherStudentBond save(TeacherStudentBond bond) {
        BondJpaEntity existing = jpa.findById(bond.id().value()).orElse(null);
        BondJpaEntity entity = BondJpaMapper.toEntity(bond, existing);
        return BondJpaMapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<TeacherStudentBond> findById(BondId id) {
        return jpa.findById(id.value()).map(BondJpaMapper::toDomain);
    }

    @Override
    public Optional<TeacherStudentBond> findActiveByStudent(UserId studentId) {
        return jpa.findByStudentIdAndEndedAtIsNull(studentId.value()).map(BondJpaMapper::toDomain);
    }

    @Override
    public Page<TeacherStudentBond> findByInstructor(UserId instructorId, Pageable pageable) {
        return jpa.findByInstructorId(instructorId.value(), pageable).map(BondJpaMapper::toDomain);
    }

    @Override
    public Page<TeacherStudentBond> findByStudent(UserId studentId, Pageable pageable) {
        return jpa.findByStudentId(studentId.value(), pageable).map(BondJpaMapper::toDomain);
    }
}
