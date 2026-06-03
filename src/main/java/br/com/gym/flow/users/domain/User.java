package br.com.gym.flow.users.domain;

import br.com.gym.flow.shared.domain.*;
import br.com.gym.flow.users.events.*;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public class User extends AggregateRoot<UserId> {

    private String name;
    private final Email email;
    private PhoneNumber phone;
    private BirthDate birthDate;
    private final Role role;
    private UserStatus status;
    private final UserId createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(
        final UserId id,
        final String name,
        final Email email,
        final PhoneNumber phone,
        final BirthDate birthDate,
        final Role role,
        final UserStatus status,
        final UserId createdBy,
        final Instant createdAt,
        final Instant updatedAt
    ) {
        super(id);
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.role = role;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User hydrate(
        final UserId id,
        final String name,
        final Email email,
        final PhoneNumber phone,
        final BirthDate birthDate,
        final Role role,
        final UserStatus status,
        final UserId createdBy,
        final Instant createdAt,
        final Instant updatedAt
    ) {
        return new User(id, name, email, phone, birthDate, role, status, createdBy, createdAt, updatedAt);
    }

    public static User registerStudent(final UserRegistrationData data, final UserId createdBy, final Role createdByRole, final Clock clock) {
        return register(Role.STUDENT, data, createdBy, createdByRole, clock);
    }

    public static User registerInstructor(final UserRegistrationData data, final UserId createdBy, final Clock clock) {
        return register(Role.INSTRUCTOR, data, createdBy, Role.ADMINISTRATOR, clock);
    }

    public static User registerAdministrator(final UserRegistrationData data, final UserId createdBy, final Clock clock) {
        return register(Role.ADMINISTRATOR, data, createdBy, Role.ADMINISTRATOR, clock);
    }

    private static User register(final Role role, final UserRegistrationData data, final UserId createdBy, final Role createdByRole, final Clock clock) {
        final Instant now = Instant.now(clock);
        final User user = new User(UserId.newId(), data.name(), data.email(), data.phone(), data.birthDate(), role, UserStatus.PENDING_FIRST_ACCESS, createdBy, now, now);

        user.registerEvent(UserRegistered.of(
            user.id().value(), user.email.value(), user.role.name(), user.name,
            createdBy == null ? null : createdBy.value(),
            createdByRole == null ? null : createdByRole.name(),
            now));
        return user;
    }

    public Result<Void> updateProfile(final String newName, final String newPhoneRaw, final LocalDate newBirthDate, final Clock clock) {
        final Notification notification = Notification.empty();
        final List<String> changed = new ArrayList<>();

        final String resolvedName = resolveName(newName, notification, changed);
        final PhoneNumber resolvedPhone = resolvePhone(newPhoneRaw, notification, changed);
        final BirthDate resolvedBirth = resolveBirthDate(newBirthDate, clock, notification, changed);

        if (notification.hasErrors()) return Result.failure(notification);
        if (changed.isEmpty()) return Result.ok();

        final Instant now = Instant.now(clock);

        this.name = resolvedName;
        this.phone = resolvedPhone;
        this.birthDate = resolvedBirth;
        this.updatedAt = now;
        registerEvent(UserProfileUpdated.of(id().value(), changed, now));
        return Result.ok();
    }

    private String resolveName(final String candidate, final Notification notification, final List<String> changed) {
        if (candidate == null) return this.name;
        if (candidate.isBlank()) {
            notification.addError("name", ErrorCode.BLANK_NAME);
            return this.name;
        }
        final String trimmed = candidate.trim();
        if (trimmed.equals(this.name)) return this.name;
        changed.add("name");
        return trimmed;
    }

    private PhoneNumber resolvePhone(final String raw, final Notification notification, final List<String> changed) {
        if (raw == null) return this.phone;
        try {
            final PhoneNumber candidate = PhoneNumber.of(raw);
            if (candidate.equals(this.phone)) return this.phone;
            changed.add("phone");
            return candidate;
        } catch (IllegalArgumentException ex) {
            notification.addError("phone", ErrorCode.INVALID_PHONE);
            return this.phone;
        }
    }

    private BirthDate resolveBirthDate(final LocalDate candidate, final Clock clock,
                                       final Notification notification, final List<String> changed) {
        if (candidate == null) return this.birthDate;
        try {
            final BirthDate newBirth = BirthDate.of(candidate, clock);
            if (newBirth.equals(this.birthDate)) return this.birthDate;
            changed.add("birthDate");
            return newBirth;
        } catch (IllegalArgumentException ex) {
            notification.addError("birthDate", ErrorCode.INVALID_BIRTH_DATE);
            return this.birthDate;
        }
    }

    public Result<Void> activate(final Clock clock) {
        return transitionTo(UserStatus.ACTIVE, clock);
    }

    public Result<Void> deactivate(final Clock clock) {
        return transitionTo(UserStatus.INACTIVE, clock);
    }

    public Result<Void> block(final Clock clock) {
        return transitionTo(UserStatus.BLOCKED, clock);
    }

    private Result<Void> transitionTo(final UserStatus target, final Clock clock) {
        if (this.status == target) return Result.ok();
        if (!this.status.canTransitionTo(target)) {
            return Result.failWith(ErrorCode.INVALID_USER_STATUS_TRANSITION);
        }
        final Instant now = Instant.now(clock);
        this.status = target;
        this.updatedAt = now;
        registerEvent(switch (target) {
            case ACTIVE -> UserActivated.of(id().value(), now);
            case INACTIVE -> UserDeactivated.of(id().value(), role.name(), now);
            case BLOCKED -> UserBlocked.of(id().value(), now);
            case PENDING_FIRST_ACCESS -> throw new IllegalStateException("not a transition target");
        });
        return Result.ok();
    }
}
