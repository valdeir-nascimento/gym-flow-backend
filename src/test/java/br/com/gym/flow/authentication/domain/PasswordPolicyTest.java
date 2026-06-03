package br.com.gym.flow.authentication.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private static final PwnedPasswordChecker NOT_PWNED = raw -> false;
    private static final PwnedPasswordChecker ALWAYS_PWNED = raw -> true;
    private static final String STRONG_PASSWORD = "Senha@1234"; // 10 chars, 4 classes

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenStrongPassword_whenValidating_thenSucceeds() {
        // When
        var result = PasswordPolicy.validate(STRONG_PASSWORD, NOT_PWNED);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Aa@1",                 // too short (< 10)
        "alllowercasenodigits", // 1 class only
        "ALLUPPERCASELETTERS",  // 1 class only
        "lowercaseanddigits1"   // 2 classes only
    })
    void givenWeakPassword_whenValidating_thenFailsWeakPassword(String weak) {
        // When
        final var result = PasswordPolicy.validate(weak, NOT_PWNED);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.WEAK_PASSWORD)).isTrue();
    }

    @Test
    void givenNullPassword_whenValidating_thenFailsWeakPassword() {
        // When
        final var result = PasswordPolicy.validate(null, NOT_PWNED);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.WEAK_PASSWORD)).isTrue();
    }

    @Test
    void givenStrongButLeakedPassword_whenValidating_thenFailsPwned() {
        // When — passes length/complexity, but the breach checker flags it
        final var result = PasswordPolicy.validate(STRONG_PASSWORD, ALWAYS_PWNED);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.PASSWORD_PWNED)).isTrue();
    }
}
