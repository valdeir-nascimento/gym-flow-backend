package br.com.gym.flow.authentication.presentation;

import jakarta.validation.constraints.NotBlank;

/** Self-service password-change payload (RF-015). */
public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank String newPassword,
    @NotBlank String passwordConfirmation,
    @NotBlank String currentRefreshToken
) {}
