package br.com.gym.flow.users.domain;

import br.com.gym.flow.shared.domain.Email;
import br.com.gym.flow.shared.domain.PhoneNumber;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Test Data Builder for {@link User}. {@code aUser().build()} yields a valid,
 * ACTIVE student with no pending domain events; each test overrides only the
 * axis it exercises. Built via {@link User#hydrate} so the aggregate starts
 * in an arbitrary persisted state (no UserRegistered event in the way).
 *
 * <p>Public so both the domain tests and the application/use-case tests share
 * one builder instead of each reinventing user construction.
 */
public final class UserTestBuilder {

    private UserId id = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private String name = "Maria Silva";
    private Email email = Email.of("maria@example.com");
    private PhoneNumber phone = PhoneNumber.of("+5511912345678");
    private BirthDate birthDate = new BirthDate(LocalDate.of(2000, 1, 1));
    private Role role = Role.STUDENT;
    private UserStatus status = UserStatus.ACTIVE;
    private UserId createdBy = null;
    private Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    private Instant updatedAt = Instant.parse("2026-01-01T00:00:00Z");

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withId(UserId id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public UserTestBuilder withPhone(String phone) {
        this.phone = PhoneNumber.of(phone);
        return this;
    }

    public UserTestBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    public UserTestBuilder withStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public UserTestBuilder withUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public User build() {
        return User.hydrate(id, name, email, phone, birthDate, role, status, createdBy, createdAt, updatedAt);
    }
}
