package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.UserStatus;
import org.springframework.data.domain.Pageable;

public record ListUsersQuery(Role role, UserStatus status, String search, Pageable pageable) {}
