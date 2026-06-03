package br.com.gym.flow.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CPFTest {

    // 111.444.777-35 is a well-known valid CPF (check digits computed from 111444777).
    private static final String VALID_FORMATTED = "111.444.777-35";
    private static final String VALID_DIGITS = "11144477735";

    @Test
    void givenFormattedValidCpf_whenCreating_thenStripsToDigitsOnly() {
        // When
        var cpf = CPF.of(VALID_FORMATTED);

        // Then
        assertThat(cpf.value()).isEqualTo(VALID_DIGITS);
    }

    @Test
    void givenValidCpf_whenFormatting_thenAppliesTheStandardMask() {
        // When / Then
        assertThat(CPF.of(VALID_DIGITS).formatted()).isEqualTo(VALID_FORMATTED);
    }

    @Test
    void givenValidCpf_whenMasking_thenHidesAllButLastTwoDigits() {
        // When / Then
        assertThat(CPF.of(VALID_DIGITS).masked()).isEqualTo("***.***.***-35");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "111.444.777-00", // wrong check digits
        "11111111111",    // all same digit
        "123",            // too short
        "529.982.247-20"  // valid base, wrong checksum
    })
    void givenInvalidCpf_whenCreating_thenThrows(String invalid) {
        assertThatThrownBy(() -> CPF.of(invalid)).isInstanceOf(IllegalArgumentException.class);
    }
}
