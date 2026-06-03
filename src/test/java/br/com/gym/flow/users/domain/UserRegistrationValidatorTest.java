package br.com.gym.flow.users.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegistrationValidatorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate VALID_BIRTH = LocalDate.of(2000, 1, 1);

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenAllValidFields_whenValidating_thenSucceedsWithNormalizedValueObjects() {
        // When
        final var result = UserRegistrationValidator.validate("  João Lima  ", "JOAO@Example.com", "+5511912345678", VALID_BIRTH, CLOCK);

        // Then
        assertThat(result.isSuccess()).isTrue();
        final var data = result.getOrThrow();
        assertThat(data.name()).isEqualTo("João Lima");            // trimmed
        assertThat(data.email().value()).isEqualTo("joao@example.com"); // lower-cased by Email VO
        assertThat(data.phone().value()).isEqualTo("+5511912345678");
        assertThat(data.birthDate().value()).isEqualTo(VALID_BIRTH);
    }

    @Test
    void givenEveryFieldInvalid_whenValidating_thenAccumulatesAllErrorsInOneNotification() {
        // When
        final var result = UserRegistrationValidator.validate("   ", "not-an-email", "abc", LocalDate.of(2030, 1, 1), CLOCK);

        // Then
        final var notification = failureOf(result);
        assertThat(notification.hasAnyCode(ErrorCode.BLANK_NAME)).isTrue();
        assertThat(notification.hasAnyCode(ErrorCode.INVALID_EMAIL)).isTrue();
        assertThat(notification.hasAnyCode(ErrorCode.INVALID_PHONE)).isTrue();
        assertThat(notification.hasAnyCode(ErrorCode.INVALID_BIRTH_DATE)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-an-email", "@no-local.com", "no-domain@", "missing-at.com"})
    void givenInvalidEmail_whenValidating_thenFailsWithInvalidEmail(String invalidEmail) {
        // When
        final var result = UserRegistrationValidator.validate("João Lima", invalidEmail, "+5511912345678", VALID_BIRTH, CLOCK);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_EMAIL)).isTrue();
    }
}
