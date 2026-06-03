package br.com.gym.flow.users.presentation;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatusRequest(@NotBlank String status) {}
