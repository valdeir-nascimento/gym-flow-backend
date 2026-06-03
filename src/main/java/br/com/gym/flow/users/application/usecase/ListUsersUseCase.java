package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserFilter;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.gym.flow.users.application.service.UserViewMapper;


@Service
@RequiredArgsConstructor
public class ListUsersUseCase implements QueryUseCase<ListUsersQuery, Page<UserView>> {

    private final UserRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Result<Page<UserView>> execute(ListUsersQuery query) {
        UserFilter filter = new UserFilter(query.role(), query.status(), query.search());
        Page<UserView> page = repository.search(filter, query.pageable()).map(UserViewMapper::toView);
        return Result.success(page);
    }
}
