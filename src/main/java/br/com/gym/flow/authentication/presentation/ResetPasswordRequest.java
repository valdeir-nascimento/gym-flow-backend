package br.com.gym.flow.authentication.presentation;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank String newPassword, @NotBlank String passwordConfirmation) {}
