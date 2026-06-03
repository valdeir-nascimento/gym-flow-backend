package br.com.gym.flow.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostalCodeTest {

    @Test
    void givenFormattedCep_whenCreating_thenStripsToDigitsOnly() {
        // When
        var cep = PostalCode.of("01310-100");

        // Then
        assertThat(cep.value()).isEqualTo("01310100");
    }

    @Test
    void givenDigits_whenFormatting_thenInsertsTheHyphen() {
        // When / Then
        assertThat(PostalCode.of("01310100").formatted()).isEqualTo("01310-100");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789", "abc"})
    void givenWrongNumberOfDigits_whenCreating_thenThrows(String invalid) {
        assertThatThrownBy(() -> PostalCode.of(invalid)).isInstanceOf(IllegalArgumentException.class);
    }
}
