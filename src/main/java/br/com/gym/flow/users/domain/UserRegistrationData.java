package br.com.gym.flow.users.domain;

import br.com.gym.flow.shared.domain.Email;
import br.com.gym.flow.shared.domain.PhoneNumber;

/**
 * Already-validated input ready to be handed to a {@link User} factory.
 * Carries value objects (not raw strings) — once a {@code UserRegistrationData}
 * exists, the data inside it is guaranteed well-formed.
 */
public record UserRegistrationData(String name, Email email, PhoneNumber phone, BirthDate birthDate) {}
