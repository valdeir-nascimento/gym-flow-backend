package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.UserId;

import java.time.LocalDate;

public record UpdateOwnProfileCommand(UserId userId, String name, String phone, LocalDate birthDate) {}
