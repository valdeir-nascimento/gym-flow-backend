package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserFilter;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static br.com.gym.flow.users.domain.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private UserRepository repository;

    private ListUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUsersUseCase(repository);
    }

    @Test
    void givenMatchingUsers_whenListing_thenReturnsMappedPage() {
        // Given
        User user = aUser().build();
        Page<User> page = new PageImpl<>(List.of(user), PAGEABLE, 1);
        when(repository.search(any(UserFilter.class), eq(PAGEABLE))).thenReturn(page);

        // When
        var result = useCase.execute(new ListUsersQuery(Role.STUDENT, UserStatus.ACTIVE, "ma", PAGEABLE));

        // Then — the entities were mapped to views, preserving the page metadata
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getTotalElements()).isEqualTo(1);
        assertThat(result.getOrThrow().getContent()).singleElement()
            .satisfies(view -> assertThat(view.id()).isEqualTo(user.id().value()));
    }

    @Test
    void givenFilterParams_whenListing_thenForwardsThemAsAUserFilter() {
        // Given
        when(repository.search(any(UserFilter.class), any())).thenReturn(Page.empty());

        // When
        useCase.execute(new ListUsersQuery(Role.INSTRUCTOR, UserStatus.BLOCKED, "joana", PAGEABLE));

        // Then — query fields are forwarded verbatim into the domain filter
        var captor = ArgumentCaptor.forClass(UserFilter.class);
        org.mockito.Mockito.verify(repository).search(captor.capture(), eq(PAGEABLE));
        assertThat(captor.getValue().role()).isEqualTo(Role.INSTRUCTOR);
        assertThat(captor.getValue().status()).isEqualTo(UserStatus.BLOCKED);
        assertThat(captor.getValue().search()).isEqualTo("joana");
    }
}
