package br.com.gym.flow.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    void givenAlreadyNormalizedNumber_whenCreating_thenKeepsIt() {
        // When / Then — 11-digit mobile with the +55 country code
        assertThat(PhoneNumber.of("+5511912345678").value()).isEqualTo("+5511912345678");
    }

    @Test
    void givenFormattedLocalNumber_whenCreating_thenStripsAndPrependsCountryCode() {
        // When — masked local input without country code
        var phone = PhoneNumber.of("(11) 91234-5678");

        // Then — normalized to E.164 with +55
        assertThat(phone.value()).isEqualTo("+5511912345678");
    }

    @Test
    void givenNormalizedNumber_whenFormatting_thenAppliesTheHumanReadableMask() {
        // When / Then
        assertThat(PhoneNumber.of("+5511912345678").formatted()).isEqualTo("+55 (11) 91234-5678");
    }

    @ParameterizedTest
    @ValueSource(strings = {"+5511912", "abc", "+551191234567890"})
    void givenInvalidNumber_whenCreating_thenThrows(String invalid) {
        assertThatThrownBy(() -> PhoneNumber.of(invalid)).isInstanceOf(IllegalArgumentException.class);
    }
}
