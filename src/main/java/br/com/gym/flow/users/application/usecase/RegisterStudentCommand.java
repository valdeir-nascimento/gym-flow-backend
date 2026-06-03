package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.UserId;

import java.time.LocalDate;

public record RegisterStudentCommand(
    String name,
    String email,
    String phone,
    LocalDate birthDate,
    UserId createdBy,
    Role createdByRole
) {}
