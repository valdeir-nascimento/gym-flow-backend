package br.com.gym.flow.users.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserStatusTest {

    @Nested
    class CanTransitionTo {

        // The full 4x4 transition matrix — one row per (from, to) pair so a
        // regression in any single edge of the state machine fails one case.
        @ParameterizedTest(name = "{0} -> {1} == {2}")
        @CsvSource({
            "PENDING_FIRST_ACCESS, ACTIVE,               true",
            "PENDING_FIRST_ACCESS, INACTIVE,             true",
            "PENDING_FIRST_ACCESS, BLOCKED,              false",
            "PENDING_FIRST_ACCESS, PENDING_FIRST_ACCESS, false",
            "ACTIVE,               INACTIVE,             true",
            "ACTIVE,               BLOCKED,              true",
            "ACTIVE,               ACTIVE,               false",
            "ACTIVE,               PENDING_FIRST_ACCESS, false",
            "BLOCKED,              ACTIVE,               true",
            "BLOCKED,              INACTIVE,             true",
            "BLOCKED,              BLOCKED,              false",
            "BLOCKED,              PENDING_FIRST_ACCESS, false",
            "INACTIVE,             ACTIVE,               true",
            "INACTIVE,             INACTIVE,             false",
            "INACTIVE,             BLOCKED,              false",
            "INACTIVE,             PENDING_FIRST_ACCESS, false",
        })
        void givenCurrentStatus_whenCheckingTransition_thenMatchesPolicy(
            UserStatus from, UserStatus to, boolean allowed) {
            assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
        }
    }

    @Nested
    class CanAuthenticate {

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = "ACTIVE")
        void givenActiveStatus_whenCheckingAuth_thenAllowed(UserStatus status) {
            assertThat(status.canAuthenticate()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
        void givenNonActiveStatus_whenCheckingAuth_thenDenied(UserStatus status) {
            assertThat(status.canAuthenticate()).isFalse();
        }
    }
}
