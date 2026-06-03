package br.com.gym.flow.users.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterInstructorRequest(
    @NotBlank String name,
    @NotBlank String email,
    @NotBlank String phone,
    @NotNull LocalDate birthDate,
    UUID createdBy
) {}
