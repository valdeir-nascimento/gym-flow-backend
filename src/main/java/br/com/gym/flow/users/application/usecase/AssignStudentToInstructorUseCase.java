package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.service.BondViewMapper;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.bond.TeacherStudentBond;
import br.com.gym.flow.users.domain.spi.BondView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AssignStudentToInstructorUseCase implements CommandUseCase<AssignStudentToInstructorCommand, BondView> {

    private final UserRepository users;
    private final BondRepository bonds;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<BondView> execute(AssignStudentToInstructorCommand command) {
        Optional<User> maybeStudent = users.findById(command.studentId());
        Optional<User> maybeInstructor = users.findById(command.instructorId());

        if (maybeStudent.isEmpty() || maybeInstructor.isEmpty()) {
            return Result.failWith(ErrorCode.USER_NOT_FOUND);
        }

        User student = maybeStudent.get();
        User instructor = maybeInstructor.get();

        if (student.role() != Role.STUDENT || instructor.role() != Role.INSTRUCTOR) {
            return Result.failWith(ErrorCode.FORBIDDEN_ROLE);
        }
        if (student.status() == UserStatus.INACTIVE || instructor.status() == UserStatus.INACTIVE) {
            return Result.failWith(ErrorCode.BOND_INACTIVE_PARTICIPANT);
        }

        if (bonds.findActiveByStudent(command.studentId()).isPresent()) {
            return Result.failWith(ErrorCode.BOND_STUDENT_HAS_ACTIVE_INSTRUCTOR);
        }

        Result<TeacherStudentBond> built = TeacherStudentBond.open(
            command.studentId(),
            command.instructorId(),
            command.createdBy(), clock
        );

        if (!built.isSuccess()) {
            return Result.failure(((Result.Failure<TeacherStudentBond>) built).notification());
        }

        TeacherStudentBond bond = built.getOrThrow();
        TeacherStudentBond saved = bonds.save(bond);
        bond.pullDomainEvents().forEach(events::publishEvent);
        return Result.success(BondViewMapper.toView(saved));
    }
}
