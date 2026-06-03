package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.UserId;

public record TransferBondCommand(UserId studentId, UserId newInstructorId, UserId actor) {}
