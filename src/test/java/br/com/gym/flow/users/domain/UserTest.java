package br.com.gym.flow.users.domain;

import br.com.gym.flow.shared.domain.*;
import br.com.gym.flow.users.events.UserActivated;
import br.com.gym.flow.users.events.UserBlocked;
import br.com.gym.flow.users.events.UserProfileUpdated;
import br.com.gym.flow.users.events.UserRegistered;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static br.com.gym.flow.users.domain.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Instant PERSISTED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final UserId ADMIN = UserId.of(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Nested
    class Register {

        private UserRegistrationData studentData() {
            return new UserRegistrationData(
                "João Lima",
                Email.of("joao@example.com"),
                PhoneNumber.of("+5511912345678"),
                new BirthDate(LocalDate.of(2000, 1, 1)));
        }

        @Test
        void givenStudentData_whenRegistering_thenStartsPendingFirstAccessAsStudent() {
            // When
            final var user = User.registerStudent(studentData(), ADMIN, Role.ADMINISTRATOR, CLOCK);

            // Then
            assertThat(user.status()).isEqualTo(UserStatus.PENDING_FIRST_ACCESS);
            assertThat(user.role()).isEqualTo(Role.STUDENT);
            assertThat(user.createdBy()).isEqualTo(ADMIN);
            assertThat(user.createdAt()).isEqualTo(NOW);
            assertThat(user.updatedAt()).isEqualTo(NOW);
        }

        @Test
        void givenStudentData_whenRegistering_thenRecordsUserRegisteredEvent() {
            // When
            final var user = User.registerStudent(studentData(), ADMIN, Role.ADMINISTRATOR, CLOCK);

            // Then
            final var events = user.pullDomainEvents();
            assertThat(events).singleElement().isInstanceOf(UserRegistered.class);

            final var registered = (UserRegistered) events.get(0);
            assertThat(registered.userId()).isEqualTo(user.id().value());
            assertThat(registered.role()).isEqualTo("STUDENT");
            assertThat(registered.createdByRole()).isEqualTo("ADMINISTRATOR");
        }

        @Test
        void givenInstructorData_whenRegistering_thenRoleIsInstructorCreatedByAdministrator() {
            // When
            final var user = User.registerInstructor(studentData(), ADMIN, CLOCK);

            // Then
            assertThat(user.role()).isEqualTo(Role.INSTRUCTOR);
            final var registered = (UserRegistered) user.pullDomainEvents().getFirst();
            assertThat(registered.createdByRole()).isEqualTo("ADMINISTRATOR");
        }
    }

    @Nested
    class UpdateProfile {

        @Test
        void givenNewName_whenUpdating_thenChangesNameAndRecordsChangedField() {
            // Given
            final var user = aUser().withName("Maria Silva").withUpdatedAt(PERSISTED_AT).build();

            // When
            final var result = user.updateProfile("Maria Souza", null, null, CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(user.name()).isEqualTo("Maria Souza");
            assertThat(user.updatedAt()).isEqualTo(NOW);

            final var event = (UserProfileUpdated) user.pullDomainEvents().getFirst();
            assertThat(event.changedFields()).containsExactly("name");
        }

        @Test
        void givenOnlyPhone_whenUpdating_thenRecordsOnlyPhoneAsChanged() {
            // Given
            final var user = aUser().withPhone("+5511912345678").build();

            // When
            final var result = user.updateProfile(null, "+5511988887777", null, CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(user.phone().value()).isEqualTo("+5511988887777");

            final var event = (UserProfileUpdated) user.pullDomainEvents().getFirst();
            assertThat(event.changedFields()).containsExactly("phone");
        }

        @Test
        void givenIdenticalValues_whenUpdating_thenNoChangeAndNoEvent() {
            // Given
            final var user = aUser().withName("Maria Silva").withUpdatedAt(PERSISTED_AT).build();

            // When
            final var result = user.updateProfile("Maria Silva", null, null, CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(user.updatedAt()).isEqualTo(PERSISTED_AT); // untouched — nothing actually changed
            assertThat(user.pullDomainEvents()).isEmpty();
        }

        @Test
        void givenInvalidPhone_whenUpdating_thenFailsAndLeavesUserUntouched() {
            // Given
            final var user = aUser().withName("Maria Silva").withPhone("+5511912345678").withUpdatedAt(PERSISTED_AT).build();

            // When
            final var result = user.updateProfile("Maria Souza", "not-a-phone", null, CLOCK);

            // Then
            assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_PHONE)).isTrue();
            assertThat(user.name()).isEqualTo("Maria Silva");          // no partial mutation
            assertThat(user.phone().value()).isEqualTo("+5511912345678");
            assertThat(user.updatedAt()).isEqualTo(PERSISTED_AT);
            assertThat(user.pullDomainEvents()).isEmpty();
        }

        @Test
        void givenBlankName_whenUpdating_thenFailsWithBlankName() {
            // Given
            final var user = aUser().build();

            // When
            final var result = user.updateProfile("   ", null, null, CLOCK);

            // Then
            assertThat(failureOf(result).hasAnyCode(ErrorCode.BLANK_NAME)).isTrue();
        }

        @Test
        void givenSeveralInvalidFields_whenUpdating_thenAccumulatesAllErrors() {
            // Given
            final var user = aUser().build();

            // When
            final var result = user.updateProfile("  ", "bad-phone", LocalDate.of(2030, 1, 1), CLOCK);

            // Then
            final var notification = failureOf(result);
            assertThat(notification.hasAnyCode(ErrorCode.BLANK_NAME)).isTrue();
            assertThat(notification.hasAnyCode(ErrorCode.INVALID_PHONE)).isTrue();
            assertThat(notification.hasAnyCode(ErrorCode.INVALID_BIRTH_DATE)).isTrue();
        }
    }

    @Nested
    class Activate {

        @Test
        void givenPendingUser_whenActivating_thenBecomesActiveWithEvent() {
            // Given
            final var user = aUser().withStatus(UserStatus.PENDING_FIRST_ACCESS).withUpdatedAt(PERSISTED_AT).build();

            // When
            final var result = user.activate(CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.updatedAt()).isEqualTo(NOW);
            assertThat(user.pullDomainEvents()).singleElement().isInstanceOf(UserActivated.class);
        }

        @Test
        void givenAlreadyActiveUser_whenActivating_thenIdempotentNoEvent() {
            // Given
            final var user = aUser().withStatus(UserStatus.ACTIVE).withUpdatedAt(PERSISTED_AT).build();

            // When
            final var result = user.activate(CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.updatedAt()).isEqualTo(PERSISTED_AT); // no transition happened
            assertThat(user.pullDomainEvents()).isEmpty();
        }
    }

    @Nested
    class Block {

        @Test
        void givenActiveUser_whenBlocking_thenBecomesBlockedWithEvent() {
            // Given
            final var user = aUser().withStatus(UserStatus.ACTIVE).build();

            // When
            final var result = user.block(CLOCK);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(user.status()).isEqualTo(UserStatus.BLOCKED);
            assertThat(user.pullDomainEvents()).singleElement().isInstanceOf(UserBlocked.class);
        }

        @Test
        void givenPendingUser_whenBlocking_thenFailsAndStaysPending() {
            // Given
            final var user = aUser().withStatus(UserStatus.PENDING_FIRST_ACCESS).build();

            // When
            final var result = user.block(CLOCK);

            // Then
            assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_USER_STATUS_TRANSITION)).isTrue();
            assertThat(user.status()).isEqualTo(UserStatus.PENDING_FIRST_ACCESS);
            assertThat(user.pullDomainEvents()).isEmpty();
        }
    }
}
