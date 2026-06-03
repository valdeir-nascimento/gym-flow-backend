package br.com.gym.flow.authentication.presentation;

import jakarta.validation.constraints.NotBlank;

public record RecoveryRequest(@NotBlank String email) {}
