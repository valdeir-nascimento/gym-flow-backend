package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.UserId;

/**
 * Administrator's intention to change a user's role (RF-012). {@code actorId} is
 * the acting administrator, checked against self-management.
 */
public record ChangeUserRoleCommand(UserId userId, Role targetRole, UserId actorId) {}
