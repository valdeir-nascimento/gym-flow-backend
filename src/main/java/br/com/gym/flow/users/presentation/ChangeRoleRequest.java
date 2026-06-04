package br.com.gym.flow.users.presentation;

import jakarta.validation.constraints.NotBlank;

/** Admin payload to change a user's role (RF-012). */
public record ChangeRoleRequest(@NotBlank String role) {}
