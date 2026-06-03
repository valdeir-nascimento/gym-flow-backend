package br.com.gym.flow.evolution.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;

/**
 * Temporal granularity of an evolution report (RF-008). Each value knows how to
 * map an instant to its bucket label, which is chronologically sortable as text.
 */
public enum Granularity {

    /** ISO week-based, e.g. {@code 2026-W23}. */
    WEEKLY {
        @Override
        public String bucketOf(final Instant instant) {
            var date = instant.atZone(ZoneOffset.UTC).toLocalDate();
            int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int year = date.get(IsoFields.WEEK_BASED_YEAR);
            return "%04d-W%02d".formatted(year, week);
        }
    },

    /** Calendar month, e.g. {@code 2026-06}. */
    MONTHLY {
        @Override
        public String bucketOf(final Instant instant) {
            var date = instant.atZone(ZoneOffset.UTC).toLocalDate();
            return "%04d-%02d".formatted(date.getYear(), date.getMonthValue());
        }
    };

    public abstract String bucketOf(Instant instant);
}
