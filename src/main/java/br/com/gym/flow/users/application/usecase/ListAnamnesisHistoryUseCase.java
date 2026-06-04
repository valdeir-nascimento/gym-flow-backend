package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.service.AnamnesisViewMapper;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisRepository;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.spi.AnamnesisView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAnamnesisHistoryUseCase implements QueryUseCase<ListAnamnesisHistoryQuery, List<AnamnesisView>> {

    private final AnamnesisRepository anamneses;
    private final BondRepository bonds;

    @Override
    @Transactional(readOnly = true)
    public Result<List<AnamnesisView>> execute(final ListAnamnesisHistoryQuery query) {
        // Ownership (RNF-001): the student themselves, their bonded instructor, or an Administrator.
        if (!AnamnesisAccess.canRead(bonds, query.studentId(), query.actorId(), query.actorRole())) {
            return Result.failWith(ErrorCode.ANAMNESIS_NOT_OWNED);
        }
        List<AnamnesisView> history = anamneses.findHistoryByStudent(query.studentId()).stream()
            .map(AnamnesisViewMapper::toView)
            .toList();
        return Result.success(history);
    }
}
