package br.com.gym.flow.users.domain.bond;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.events.BondCreated;
import br.com.gym.flow.users.events.BondEnded;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherStudentBondTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UserId STUDENT = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final UserId INSTRUCTOR = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final UserId ACTOR = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Nested
    class Open {

        @Test
        void givenDistinctParticipants_whenOpening_thenActiveBondWithEvent() {
            // When
            final var result = TeacherStudentBond.open(STUDENT, INSTRUCTOR, ACTOR, CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            final var bond = result.getOrThrow();
            assertThat(bond.isActive()).isTrue();
            assertThat(bond.studentId()).isEqualTo(STUDENT);
            assertThat(bond.instructorId()).isEqualTo(INSTRUCTOR);
            assertThat(bond.startedAt()).isEqualTo(NOW);
            assertThat(bond.pullDomainEvents()).singleElement().isInstanceOf(BondCreated.class);
        }

        @Test
        void givenSameStudentAndInstructor_whenOpening_thenFailsInvalidInput() {
            // When
            final var result = TeacherStudentBond.open(STUDENT, STUDENT, ACTOR, CLOCK);

            // Then
            assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
        }

        @Test
        void givenNullStudent_whenOpening_thenFailsInvalidInput() {
            // When
            final var result = TeacherStudentBond.open(null, INSTRUCTOR, ACTOR, CLOCK);

            // Then
            assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
        }
    }

    @Nested
    class Close {

        @Test
        void givenActiveBond_whenClosing_thenEndsWithEvent() {
            // Given
            final var bond = TeacherStudentBond.open(STUDENT, INSTRUCTOR, ACTOR, CLOCK).getOrThrow();
            bond.pullDomainEvents(); // drain the BondCreated from opening

            // When
            final var result = bond.close(CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(bond.isActive()).isFalse();
            assertThat(bond.endedAt()).isEqualTo(NOW);
            assertThat(bond.pullDomainEvents()).singleElement().isInstanceOf(BondEnded.class);
        }

        @Test
        void givenAlreadyClosedBond_whenClosingAgain_thenFails() {
            // Given — a bond already closed once
            final var bond = TeacherStudentBond.open(STUDENT, INSTRUCTOR, ACTOR, CLOCK).getOrThrow();
            bond.close(CLOCK);
            bond.pullDomainEvents();

            // When
            final var result = bond.close(CLOCK);

            // Then
            assertThat(failureOf(result).hasAnyCode(ErrorCode.BOND_NOT_FOUND)).isTrue();
        }
    }
}
