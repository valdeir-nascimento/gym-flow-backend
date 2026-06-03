package br.com.gym.flow.users.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BirthDateTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void givenAdultDate_whenCreating_thenSucceeds() {
        final var adult = LocalDate.of(2000, 1, 1); // 26 years old on 2026-06-01
        assertThat(BirthDate.of(adult, CLOCK).value()).isEqualTo(adult);
    }

    @Test
    void givenExactlyMinimumAge_whenCreating_thenSucceeds() {
        final var thirteenthBirthday = LocalDate.of(2013, 6, 1);
        assertThat(BirthDate.of(thirteenthBirthday, CLOCK).value()).isEqualTo(thirteenthBirthday);
    }

    @Test
    void givenOneDayShyOfMinimumAge_whenCreating_thenThrows() {
        final var almostThirteen = LocalDate.of(2013, 6, 2);
        assertThatThrownBy(() -> BirthDate.of(almostThirteen, CLOCK)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenToday_whenCreating_thenThrows() {
        final var today = LocalDate.of(2026, 6, 1);
        assertThatThrownBy(() -> BirthDate.of(today, CLOCK)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenFutureDate_whenCreating_thenThrows() {
        final var future = LocalDate.of(2030, 1, 1);
        assertThatThrownBy(() -> BirthDate.of(future, CLOCK)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNullCandidate_whenCreating_thenThrows() {
        assertThatThrownBy(() -> BirthDate.of(null, CLOCK)).isInstanceOf(IllegalArgumentException.class);
    }
}
