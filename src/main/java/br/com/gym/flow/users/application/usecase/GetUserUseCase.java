package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.gym.flow.users.application.service.UserViewMapper;


@Service
@RequiredArgsConstructor
public class GetUserUseCase implements QueryUseCase<GetUserQuery, UserView> {

    private final UserRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Result<UserView> execute(GetUserQuery query) {
        return Result.ofOptional(
            repository.findById(query.userId()).map(UserViewMapper::toView),
            ErrorCode.USER_NOT_FOUND);
    }
}
