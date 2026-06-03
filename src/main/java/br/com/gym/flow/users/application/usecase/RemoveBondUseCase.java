package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
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
public class RemoveBondUseCase implements CommandUseCase<RemoveBondCommand, BondView> {

    private final BondRepository bonds;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<BondView> execute(RemoveBondCommand command) {
        Optional<TeacherStudentBond> maybe = bonds.findById(command.bondId());
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.BOND_NOT_FOUND);
        }
        TeacherStudentBond bond = maybe.get();
        if (!bond.isActive()) {
            return Result.failWith(ErrorCode.BOND_NOT_FOUND, "vínculo já encerrado");
        }
        if (command.restrictToInstructor() && !bond.instructorId().equals(command.actor())) {
            return Result.failWith(ErrorCode.BOND_NOT_OWNED_BY_INSTRUCTOR);
        }
        Result<Void> close = bond.close(clock);
        if (!close.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) close).notification());
        }
        TeacherStudentBond saved = bonds.save(bond);
        bond.pullDomainEvents().forEach(events::publishEvent);
        return Result.success(BondViewMapper.toView(saved));
    }
}
