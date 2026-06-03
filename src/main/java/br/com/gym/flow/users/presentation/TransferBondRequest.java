package br.com.gym.flow.users.presentation;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferBondRequest(
    @NotNull UUID studentId,
    @NotNull UUID newInstructorId,
    @NotNull UUID actor
) {}
