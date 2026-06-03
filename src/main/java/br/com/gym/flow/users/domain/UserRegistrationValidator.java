package br.com.gym.flow.users.domain;

import br.com.gym.flow.shared.domain.Email;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.PhoneNumber;
import br.com.gym.flow.shared.domain.Result;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Parses and validates the four raw fields that make up a user registration,
 * accumulating every violation into a single {@link Notification}. On
 * success, hands back a {@link UserRegistrationData} carrying value objects
 * ready to feed a {@link User} factory.
 */
public final class UserRegistrationValidator {

    private UserRegistrationValidator() {}

    public static Result<UserRegistrationData> validate(
            String name, String emailRaw, String phoneRaw, LocalDate birthDate, Clock clock) {
        Notification notification = Notification.empty();
        String parsedName = parseName(name, notification);
        Email email = parseEmail(emailRaw, notification);
        PhoneNumber phone = parsePhone(phoneRaw, notification);
        BirthDate parsedBirth = parseBirthDate(birthDate, clock, notification);
        return notification.hasErrors()
            ? Result.failure(notification)
            : Result.success(new UserRegistrationData(parsedName, email, phone, parsedBirth));
    }

    private static String parseName(String raw, Notification notification) {
        if (raw == null || raw.isBlank()) {
            notification.addError("name", ErrorCode.BLANK_NAME);
            return null;
        }
        return raw.trim();
    }

    private static Email parseEmail(String raw, Notification notification) {
        try { return Email.of(raw); }
        catch (IllegalArgumentException ex) {
            notification.addError("email", ErrorCode.INVALID_EMAIL);
            return null;
        }
    }

    private static PhoneNumber parsePhone(String raw, Notification notification) {
        try { return PhoneNumber.of(raw); }
        catch (IllegalArgumentException ex) {
            notification.addError("phone", ErrorCode.INVALID_PHONE);
            return null;
        }
    }

    private static BirthDate parseBirthDate(LocalDate date, Clock clock, Notification notification) {
        try { return BirthDate.of(date, clock); }
        catch (IllegalArgumentException ex) {
            notification.addError("birthDate", ErrorCode.INVALID_BIRTH_DATE);
            return null;
        }
    }
}
