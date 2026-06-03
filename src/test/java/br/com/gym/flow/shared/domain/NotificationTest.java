package br.com.gym.flow.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    void givenEmptyNotification_whenChecking_thenHasNoErrors() {
        // When / Then
        assertThat(Notification.empty().hasErrors()).isFalse();
    }

    @Test
    void givenAddedError_whenChecking_thenHasErrorsAndExposesTheCode() {
        // Given
        var notification = Notification.empty();

        // When
        notification.addError(ErrorCode.BLANK_NAME);

        // Then
        assertThat(notification.hasErrors()).isTrue();
        assertThat(notification.errors()).singleElement()
            .satisfies(error -> assertThat(error.code()).isEqualTo(ErrorCode.BLANK_NAME.name()));
    }

    @Test
    void givenErrors_whenQueryingByCode_thenMatchesOnlyPresentOnes() {
        // Given
        var notification = Notification.empty();
        notification.addError(ErrorCode.INVALID_EMAIL);

        // When / Then
        assertThat(notification.hasAnyCode(ErrorCode.INVALID_EMAIL)).isTrue();
        assertThat(notification.hasAnyCode(ErrorCode.INVALID_PHONE)).isFalse();
    }

    @Test
    void givenTwoNotifications_whenMerging_thenAccumulatesBothSetsOfErrors() {
        // Given
        var first = Notification.empty();
        first.addError(ErrorCode.BLANK_NAME);
        var second = Notification.empty();
        second.addError(ErrorCode.INVALID_EMAIL);

        // When
        first.merge(second);

        // Then
        assertThat(first.errors()).hasSize(2);
        assertThat(first.hasAnyCode(ErrorCode.BLANK_NAME)).isTrue();
        assertThat(first.hasAnyCode(ErrorCode.INVALID_EMAIL)).isTrue();
    }

    @Test
    void givenSingleErrorFactory_whenCreating_thenHoldsExactlyThatError() {
        // When
        var notification = Notification.ofSingle(ErrorCode.USER_NOT_FOUND);

        // Then
        assertThat(notification.errors()).singleElement()
            .satisfies(error -> assertThat(error.code()).isEqualTo(ErrorCode.USER_NOT_FOUND.name()));
    }
}
