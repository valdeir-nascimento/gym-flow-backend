package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.exercises.domain.spi.ExerciseCatalog;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.domain.TrainingItem;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import br.com.gym.flow.trainings.events.TrainingCreated;
import br.com.gym.flow.users.domain.spi.TeacherStudentDirectory;
import br.com.gym.flow.users.domain.spi.UserDirectory;
import br.com.gym.flow.users.domain.spi.UserView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTrainingUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INSTRUCTOR = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EXERCISE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final LocalDate START = LocalDate.of(2026, 6, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 31);

    @Mock
    private TrainingRepository trainings;
    @Mock
    private UserDirectory users;
    @Mock
    private TeacherStudentDirectory bonds;
    @Mock
    private ExerciseCatalog catalog;
    @Mock
    private ApplicationEventPublisher events;

    private CreateTrainingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTrainingUseCase(trainings, users, bonds, catalog, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static CreateTrainingCommand command(String name, String actorRole) {
        TrainingItem item = new TrainingItem(EXERCISE, 4, 10, new BigDecimal("40.00"), 60);
        return new CreateTrainingCommand(STUDENT, name, "Hipertrofia", START, END, List.of(item), INSTRUCTOR, actorRole);
    }

    private static UserView userView(String status) {
        return new UserView(STUDENT, "Aluno", "aluno@example.com", "+5511912345678",
            LocalDate.of(2000, 1, 1), "STUDENT", status, INSTRUCTOR,
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static ExerciseView exerciseView(String status) {
        return new ExerciseView(EXERCISE, "Supino", "CHEST", "d", "Barra", "INTERMEDIATE", null, null, status,
            INSTRUCTOR, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void givenStructurallyInvalidRequest_whenCreating_thenFailsValidationAndTouchesNothing() {
        // When — blank name fails structural validation before any cross-module work
        var result = useCase.execute(command("  ", "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
        verifyNoInteractions(users, bonds, catalog, trainings, events);
    }

    @Test
    void givenUnknownStudent_whenCreating_thenFailsNotFound() {
        // Given
        when(users.findById(STUDENT)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(command("Treino A", "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.USER_NOT_FOUND)).isTrue();
        verify(trainings, never()).save(any());
    }

    @Test
    void givenInactiveStudent_whenCreating_thenFailsStudentInactive() {
        // Given
        when(users.findById(STUDENT)).thenReturn(Optional.of(userView("INACTIVE")));

        // When
        var result = useCase.execute(command("Treino A", "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_STUDENT_INACTIVE)).isTrue();
        verify(trainings, never()).save(any());
    }

    @Test
    void givenInstructorNotLinkedToStudent_whenCreating_thenFailsForbidden() {
        // Given
        when(users.findById(STUDENT)).thenReturn(Optional.of(userView("ACTIVE")));
        when(bonds.hasActiveBond(STUDENT, INSTRUCTOR)).thenReturn(false);

        // When
        var result = useCase.execute(command("Treino A", "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_INSTRUCTOR_NOT_LINKED)).isTrue();
        verifyNoInteractions(catalog);
        verify(trainings, never()).save(any());
    }

    @Test
    void givenAdministratorActor_whenCreating_thenBypassesBondButStillRejectsInactiveExercise() {
        // Given — Admin skips the bond check, but an inactive exercise is still rejected (422)
        when(users.findById(STUDENT)).thenReturn(Optional.of(userView("ACTIVE")));
        when(catalog.findById(EXERCISE)).thenReturn(Optional.of(exerciseView("INACTIVE")));

        // When
        var result = useCase.execute(command("Treino A", "ADMINISTRATOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_INACTIVE_EXERCISE)).isTrue();
        verifyNoInteractions(bonds); // bond check bypassed for Administrator
    }

    @Test
    void givenExerciseMissingFromCatalog_whenCreating_thenFailsValidation() {
        // Given
        when(users.findById(STUDENT)).thenReturn(Optional.of(userView("ACTIVE")));
        when(bonds.hasActiveBond(STUDENT, INSTRUCTOR)).thenReturn(true);
        when(catalog.findById(EXERCISE)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(command("Treino A", "INSTRUCTOR"));

        // Then — a non-existent exercise is a validation error (400)
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
        verify(trainings, never()).save(any());
    }

    @Test
    void givenOverlappingActiveTraining_whenCreating_thenFailsOverlap() {
        // Given
        when(users.findById(STUDENT)).thenReturn(Optional.of(userView("ACTIVE")));
        when(bonds.hasActiveBond(STUDENT, INSTRUCTOR)).thenReturn(true);
        when(catalog.findById(EXERCISE)).thenReturn(Optional.of(exerciseView("ACTIVE")));
        when(trainings.existsActiveOverlapping(eq(STUDENT), any())).thenReturn(true);

        // When
        var result = useCase.execute(command("Treino A", "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_OVERLAPPING_PERIOD)).isTrue();
        verify(trainings, never()).save(any());
    }

    @Test
    void givenAllChecksPass_whenCreating_thenPersistsAndPublishesEvent() {
        // Given
        when(users.findById(STUDENT)).thenReturn(Optional.of(userView("ACTIVE")));
        when(bonds.hasActiveBond(STUDENT, INSTRUCTOR)).thenReturn(true);
        when(catalog.findById(EXERCISE)).thenReturn(Optional.of(exerciseView("ACTIVE")));
        when(trainings.existsActiveOverlapping(eq(STUDENT), any())).thenReturn(false);
        when(trainings.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command("Treino A", "INSTRUCTOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().studentId()).isEqualTo(STUDENT);
        assertThat(result.getOrThrow().items()).hasSize(1);
        verify(trainings).save(any());
        verify(events).publishEvent(any(TrainingCreated.class));
    }
}
