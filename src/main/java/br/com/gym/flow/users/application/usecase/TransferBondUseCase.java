package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
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
import br.com.gym.flow.users.application.service.BondViewMapper;


@Service
@RequiredArgsConstructor
public class TransferBondUseCase implements CommandUseCase<TransferBondCommand, BondView> {

    private final UserRepository users;
    private final BondRepository bonds;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<BondView> execute(TransferBondCommand command) {
        Optional<User> maybeStudent = users.findById(command.studentId());
        Optional<User> maybeNew = users.findById(command.newInstructorId());
        if (maybeStudent.isEmpty() || maybeNew.isEmpty()) {
            return Result.failWith(ErrorCode.USER_NOT_FOUND);
        }
        if (maybeNew.get().role() != Role.INSTRUCTOR || maybeNew.get().status() == UserStatus.INACTIVE) {
            return Result.failWith(ErrorCode.BOND_INACTIVE_PARTICIPANT);
        }

        Optional<TeacherStudentBond> active = bonds.findActiveByStudent(command.studentId());
        active.ifPresent(bond -> {
            bond.close(clock);
            bonds.save(bond);
            bond.pullDomainEvents().forEach(events::publishEvent);
        });

        Result<TeacherStudentBond> built = TeacherStudentBond.open(
            command.studentId(), command.newInstructorId(), command.actor(), clock);
        if (!built.isSuccess()) {
            return Result.failure(((Result.Failure<TeacherStudentBond>) built).notification());
        }
        TeacherStudentBond bond = built.getOrThrow();
        TeacherStudentBond saved = bonds.save(bond);
        bond.pullDomainEvents().forEach(events::publishEvent);
        return Result.success(BondViewMapper.toView(saved));
    }
}
