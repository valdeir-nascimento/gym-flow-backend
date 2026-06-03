package br.com.gym.flow.trainings.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingPeriodTest {

    private static TrainingPeriod period(String start, String end) {
        return new TrainingPeriod(LocalDate.parse(start), end == null ? null : LocalDate.parse(end));
    }

    @Test
    void givenOverlappingWindows_whenCheckingOverlap_thenTrue() {
        // [Jan 1, Jan 31] vs [Jan 15, Feb 15] share mid-January
        assertThat(period("2026-01-01", "2026-01-31").overlaps(period("2026-01-15", "2026-02-15"))).isTrue();
    }

    @Test
    void givenAdjacentNonTouchingWindows_whenCheckingOverlap_thenFalse() {
        // [Jan 1, Jan 31] vs [Feb 1, Feb 28] — no shared day
        assertThat(period("2026-01-01", "2026-01-31").overlaps(period("2026-02-01", "2026-02-28"))).isFalse();
    }

    @Test
    void givenOpenEndedWindow_whenCheckingOverlapWithLaterWindow_thenTrue() {
        // [Jan 1, ∞) vs [Mar 1, Mar 31] — the open end reaches into March
        assertThat(period("2026-01-01", null).overlaps(period("2026-03-01", "2026-03-31"))).isTrue();
    }

    @Test
    void givenClosedWindowEndingBeforeAnOpenEndedOne_whenCheckingOverlap_thenFalse() {
        // [Jan 1, Jan 31] vs [Mar 1, ∞) — closed window ended before the other began
        assertThat(period("2026-01-01", "2026-01-31").overlaps(period("2026-03-01", null))).isFalse();
    }

    @Test
    void givenDate_whenCheckingCoverage_thenRespectsInclusiveBoundsAndOpenEnd() {
        var closed = period("2026-01-01", "2026-01-31");
        assertThat(closed.covers(LocalDate.parse("2026-01-15"))).isTrue();
        assertThat(closed.covers(LocalDate.parse("2026-01-01"))).isTrue();  // inclusive start
        assertThat(closed.covers(LocalDate.parse("2026-01-31"))).isTrue();  // inclusive end
        assertThat(closed.covers(LocalDate.parse("2026-02-01"))).isFalse();

        assertThat(period("2026-01-01", null).covers(LocalDate.parse("2030-12-31"))).isTrue(); // open-ended
    }
}
