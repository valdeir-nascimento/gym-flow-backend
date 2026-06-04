package br.com.gym.flow.users.domain.anamnesis;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnamnesisValidatorTest {

    private static Result<AnamnesisDraft> validate(final BigDecimal weight, final int height) {
        return AnamnesisValidator.validate(weight, height, "Hipertrofia", "iniciante",
            List.of("lesão no ombro"), List.of("hipertensão"), List.of(), "obs");
    }

    @Test
    void givenPlausibleMeasures_whenValidating_thenSucceedsAndTrims() {
        var result = validate(new BigDecimal("80.00"), 175);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().weight().kilograms()).isEqualByComparingTo("80.00");
        assertThat(result.getOrThrow().height().centimeters()).isEqualTo(175);
        assertThat(result.getOrThrow().injuries()).containsExactly("lesão no ombro");
    }

    @Test
    void givenImplausibleWeight_whenValidating_thenFailsWeightOutOfRange() {
        var result = validate(new BigDecimal("5"), 175);

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?>) result).notification().hasAnyCode(ErrorCode.ANAMNESIS_IMPLAUSIBLE_WEIGHT)).isTrue();
    }

    @Test
    void givenImplausibleHeight_whenValidating_thenFailsHeightOutOfRange() {
        var result = validate(new BigDecimal("80.00"), 300);

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?>) result).notification().hasAnyCode(ErrorCode.ANAMNESIS_IMPLAUSIBLE_HEIGHT)).isTrue();
    }
}
