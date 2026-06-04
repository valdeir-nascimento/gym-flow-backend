package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserStatus;

public record ChangeUserStatusCommand(UserId userId, UserStatus targetStatus, UserId actorId) {}
