package br.com.gym.flow.users.presentation;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignBondRequest(
    @NotNull UUID studentId,
    @NotNull UUID instructorId,
    @NotNull UUID createdBy
) {}
