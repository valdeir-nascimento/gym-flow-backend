package br.com.gym.flow.users.domain;

import br.com.gym.flow.shared.domain.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId id);

    /** Batch lookup by ids (RF-010). */
    List<User> findByIds(Collection<UserId> ids);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    Page<User> search(UserFilter filter, Pageable pageable);

    long countActiveAdministrators();
}
