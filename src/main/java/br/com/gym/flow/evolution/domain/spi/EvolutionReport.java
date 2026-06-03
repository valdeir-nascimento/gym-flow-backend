package br.com.gym.flow.evolution.domain.spi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read model of a student's evolution over a period (RF-008). All series are
 * ordered by bucket (oldest first) and may be empty when there is no data —
 * absence of indicators is not an error.
 */
public record EvolutionReport(
    UUID studentId,
    String granularity,
    Instant from,
    Instant to,
    List<FrequencyPoint> frequency,
    List<VolumePoint> volume,
    List<ExerciseOneRepMaxSeries> oneRepMaxByExercise,
    List<BodyweightPoint> bodyweight
) {

    /** Number of workouts performed in the bucket (frequency). */
    public record FrequencyPoint(String bucket, long workouts) {}

    /** Total training volume (Σ sets × reps × load) in the bucket. */
    public record VolumePoint(String bucket, java.math.BigDecimal totalVolume) {}

    /** Estimated 1RM time series for a single exercise. */
    public record ExerciseOneRepMaxSeries(UUID exerciseId, List<OneRepMaxPoint> points) {}

    /** Best estimated one-rep-max for the exercise in the bucket. */
    public record OneRepMaxPoint(String bucket, java.math.BigDecimal estimatedOneRepMax) {}

    /** Body weight in the bucket (sourced from the initial anamnesis — RF-017). */
    public record BodyweightPoint(String bucket, java.math.BigDecimal weightKg) {}
}
