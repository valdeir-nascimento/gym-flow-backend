package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.spi.BondView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.gym.flow.users.application.service.BondViewMapper;


@Service
@RequiredArgsConstructor
public class ListBondsUseCase implements QueryUseCase<ListBondsQuery, Page<BondView>> {

    private final BondRepository bonds;

    @Override
    @Transactional(readOnly = true)
    public Result<Page<BondView>> execute(ListBondsQuery query) {
        if (query.instructorId() != null) {
            return Result.success(bonds.findByInstructor(query.instructorId(), query.pageable())
                .map(BondViewMapper::toView));
        }
        if (query.studentId() != null) {
            return Result.success(bonds.findByStudent(query.studentId(), query.pageable())
                .map(BondViewMapper::toView));
        }
        return Result.failWith(ErrorCode.INVALID_INPUT, "informe instructorId ou studentId");
    }
}
