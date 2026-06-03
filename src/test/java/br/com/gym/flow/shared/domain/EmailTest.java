package br.com.gym.flow.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void givenMixedCaseWithSpaces_whenCreating_thenTrimsAndLowercases() {
        // When
        var email = Email.of("  Maria@Example.COM  ");

        // Then
        assertThat(email.value()).isEqualTo("maria@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "no-at-sign", "@no-local.com", "no-domain@", "missing-tld@x", "a b@x.com"})
    void givenMalformedEmail_whenCreating_thenThrows(String invalid) {
        assertThatThrownBy(() -> Email.of(invalid)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNull_whenCreating_thenThrows() {
        assertThatThrownBy(() -> Email.of(null)).isInstanceOf(NullPointerException.class);
    }
}
