package br.com.gym.flow.users.presentation;

import java.time.LocalDate;

public record UpdateOwnProfileRequest(String name, String phone, LocalDate birthDate) {}
