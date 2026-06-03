package br.com.gym.flow.evolution.domain;

import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport.ExerciseOneRepMaxSeries;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport.FrequencyPoint;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport.OneRepMaxPoint;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport.VolumePoint;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionItemView;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Computes the evolution indicators (RF-008) from a student's executions:
 * <ul>
 *   <li><b>frequency</b> — workouts per bucket;</li>
 *   <li><b>volume</b> — Σ (sets × reps × load) per bucket;</li>
 *   <li><b>estimated 1RM per exercise</b> — best Epley estimate per bucket.</li>
 * </ul>
 * Body weight comes exclusively from the initial anamnesis (RF-017) and is left
 * empty until that source exists. Items without a load (bodyweight movements) do
 * not contribute to volume or 1RM. Pure and side-effect free.
 */
public final class EvolutionCalculator {

    private static final BigDecimal EPLEY_DIVISOR = BigDecimal.valueOf(30);
    private static final int INTERNAL_SCALE = 4;
    private static final int RESULT_SCALE = 2;

    private EvolutionCalculator() {
    }

    public static EvolutionReport compute(
        final UUID studentId,
        final Granularity granularity,
        final Instant from,
        final Instant to,
        final List<WorkoutExecutionView> executions
    ) {
        Map<String, Long> frequency = new TreeMap<>();
        Map<String, BigDecimal> volume = new TreeMap<>();
        Map<UUID, Map<String, BigDecimal>> oneRepMax = new TreeMap<>();

        for (WorkoutExecutionView execution : executions) {
            String bucket = granularity.bucketOf(execution.startedAt());
            frequency.merge(bucket, 1L, Long::sum);

            for (WorkoutExecutionItemView item : execution.items()) {
                if (item.load() == null) {
                    continue; // bodyweight movement: no load to aggregate
                }
                volume.merge(bucket, volumeOf(item), BigDecimal::add);

                if (item.repetitions() > 0) {
                    BigDecimal estimate = estimatedOneRepMax(item.load(), item.repetitions());
                    oneRepMax
                        .computeIfAbsent(item.exerciseId(), id -> new TreeMap<>())
                        .merge(bucket, estimate, EvolutionCalculator::max);
                }
            }
        }

        return new EvolutionReport(
            studentId,
            granularity.name(),
            from,
            to,
            toFrequencyPoints(frequency),
            toVolumePoints(volume),
            toOneRepMaxSeries(oneRepMax),
            List.of()
        );
    }

    /** Epley formula: 1RM = load × (1 + reps / 30). */
    private static BigDecimal estimatedOneRepMax(final BigDecimal load, final int reps) {
        BigDecimal factor = BigDecimal.ONE.add(
            BigDecimal.valueOf(reps).divide(EPLEY_DIVISOR, INTERNAL_SCALE, RoundingMode.HALF_UP));
        return load.multiply(factor).setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal volumeOf(final WorkoutExecutionItemView item) {
        return item.load()
            .multiply(BigDecimal.valueOf(item.sets()))
            .multiply(BigDecimal.valueOf(item.repetitions()))
            .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal max(final BigDecimal a, final BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static List<FrequencyPoint> toFrequencyPoints(final Map<String, Long> frequency) {
        List<FrequencyPoint> points = new ArrayList<>(frequency.size());
        frequency.forEach((bucket, count) -> points.add(new FrequencyPoint(bucket, count)));
        return points;
    }

    private static List<VolumePoint> toVolumePoints(final Map<String, BigDecimal> volume) {
        List<VolumePoint> points = new ArrayList<>(volume.size());
        volume.forEach((bucket, total) -> points.add(new VolumePoint(bucket, total)));
        return points;
    }

    private static List<ExerciseOneRepMaxSeries> toOneRepMaxSeries(final Map<UUID, Map<String, BigDecimal>> oneRepMax) {
        List<ExerciseOneRepMaxSeries> series = new ArrayList<>(oneRepMax.size());
        oneRepMax.forEach((exerciseId, byBucket) -> {
            List<OneRepMaxPoint> points = new ArrayList<>(byBucket.size());
            byBucket.forEach((bucket, estimate) -> points.add(new OneRepMaxPoint(bucket, estimate)));
            series.add(new ExerciseOneRepMaxSeries(exerciseId, points));
        });
        return series;
    }
}
