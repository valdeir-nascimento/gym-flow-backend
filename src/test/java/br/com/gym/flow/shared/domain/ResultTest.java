package br.com.gym.flow.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTest {

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenSuccess_whenMapping_thenTransformsTheValue() {
        // When
        var result = Result.success(2).map(value -> value + 1);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow()).isEqualTo(3);
    }

    @Test
    void givenFailure_whenMapping_thenStaysFailureAndSkipsTheFunction() {
        // Given
        Result<Integer> failure = Result.failWith(ErrorCode.INVALID_INPUT);

        // When
        var result = failure.map(value -> value + 1);

        // Then — the mapping function never ran; the original error is preserved
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
    }

    @Test
    void givenSuccess_whenFlatMapping_thenChainsIntoTheNextResult() {
        // When
        var result = Result.success(2).flatMap(value -> Result.success("value=" + value));

        // Then
        assertThat(result.getOrThrow()).isEqualTo("value=2");
    }

    @Test
    void givenFailure_whenFlatMapping_thenShortCircuits() {
        // Given
        Result<Integer> failure = Result.failWith(ErrorCode.INVALID_INPUT);

        // When — the function would have produced a success, but it must not run
        var result = failure.flatMap(value -> Result.success("got " + value));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
    }

    @Test
    void givenFailure_whenGetOrThrow_thenThrowsIllegalState() {
        // Given
        Result<String> failure = Result.failWith(ErrorCode.USER_NOT_FOUND);

        // When / Then
        assertThatThrownBy(failure::getOrThrow).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenFailure_whenOrElseThrow_thenThrowsTheSuppliedException() {
        // Given
        Result<String> failure = Result.failWith(ErrorCode.USER_NOT_FOUND);

        // When / Then — the caller's exception factory wins
        assertThatThrownBy(() -> failure.orElseThrow(notification -> new IllegalArgumentException("boom")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("boom");
    }

    @Test
    void givenPresentOptional_whenOfOptional_thenSucceedsWithTheValue() {
        // When
        var result = Result.ofOptional(Optional.of("found"), ErrorCode.USER_NOT_FOUND);

        // Then
        assertThat(result.getOrThrow()).isEqualTo("found");
    }

    @Test
    void givenEmptyOptional_whenOfOptional_thenFailsWithTheGivenCode() {
        // When
        var result = Result.ofOptional(Optional.empty(), ErrorCode.USER_NOT_FOUND);

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_FOUND)).isTrue();
    }
}
