package br.com.gym.flow.users.domain.anamnesis;

/**
 * Body height in centimeters (RF-017). Guards a plausible human range; out-of-range
 * values are rejected so the use case can surface a 422.
 */
public record Height(int centimeters) {

    private static final int MIN_CM = 50;
    private static final int MAX_CM = 260;

    public static Height of(final int centimeters) {
        if (centimeters < MIN_CM || centimeters > MAX_CM) {
            throw new IllegalArgumentException("implausible height: " + centimeters);
        }
        return new Height(centimeters);
    }
}
