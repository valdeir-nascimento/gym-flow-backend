package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.exercises.domain.spi.ExerciseCatalog;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.domain.TrainingId;
import br.com.gym.flow.trainings.domain.TrainingItem;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import br.com.gym.flow.trainings.domain.TrainingStatus;
import br.com.gym.flow.trainings.events.TrainingUpdated;
import br.com.gym.flow.users.domain.spi.AnamnesisDirectory;
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

import static br.com.gym.flow.trainings.domain.TrainingTestBuilder.EXERCISE_ID;
import static br.com.gym.flow.trainings.domain.TrainingTestBuilder.INSTRUCTOR_ID;
import static br.com.gym.flow.trainings.domain.TrainingTestBuilder.STUDENT_ID;
import static br.com.gym.flow.trainings.domain.TrainingTestBuilder.aTraining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTrainingUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final TrainingId TRAINING_ID = TrainingId.of(UUID.fromString("00000000-0000-0000-0000-000000000010"));
    private static final LocalDate START = LocalDate.of(2026, 6, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 31);

    @Mock
    private TrainingRepository trainings;
    @Mock
    private ExerciseCatalog catalog;
    @Mock
    private AnamnesisDirectory anamnesis;
    @Mock
    private ApplicationEventPublisher events;

    private UpdateTrainingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTrainingUseCase(trainings, catalog, anamnesis, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static UpdateTrainingCommand command(String name, TrainingStatus status, UUID actorId, String actorRole) {
        TrainingItem item = new TrainingItem(EXERCISE_ID, 4, 10, new BigDecimal("40.00"), 60);
        return new UpdateTrainingCommand(TRAINING_ID, name, "Hipertrofia", START, END, List.of(item), status, actorId, actorRole);
    }

    private static ExerciseView activeExercise() {
        return new ExerciseView(EXERCISE_ID, "Supino", "CHEST", "d", "Barra", "INTERMEDIATE", null, null, "ACTIVE",
            INSTRUCTOR_ID, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void givenUnknownTraining_whenUpdating_thenFailsNotFound() {
        // Given
        when(trainings.findById(TRAINING_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(command("Treino B", null, INSTRUCTOR_ID, "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_NOT_FOUND)).isTrue();
        verify(trainings, never()).save(any());
    }

    @Test
    void givenInstructorWhoIsNotTheOwner_whenUpdating_thenFailsNotOwned() {
        // Given — an instructor other than the training's owner
        when(trainings.findById(TRAINING_ID)).thenReturn(Optional.of(aTraining().withId(TRAINING_ID).build()));

        // When
        var result = useCase.execute(command("Treino B", null, UUID.randomUUID(), "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_NOT_OWNED)).isTrue();
        verify(trainings, never()).save(any());
        verifyNoInteractions(catalog);
    }

    @Test
    void givenAdministratorWhoIsNotTheOwner_whenUpdating_thenAllowed() {
        // Given — Administrator bypasses ownership
        when(trainings.findById(TRAINING_ID)).thenReturn(Optional.of(aTraining().withId(TRAINING_ID).build()));
        when(catalog.findById(EXERCISE_ID)).thenReturn(Optional.of(activeExercise()));
        when(trainings.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command("Treino B", null, UUID.randomUUID(), "ADMINISTRATOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().name()).isEqualTo("Treino B");
    }

    @Test
    void givenBlankName_whenUpdating_thenFailsValidationBeforeCatalog() {
        // Given — owner, but a blank name
        when(trainings.findById(TRAINING_ID)).thenReturn(Optional.of(aTraining().withId(TRAINING_ID).build()));

        // When
        var result = useCase.execute(command("  ", null, INSTRUCTOR_ID, "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.INVALID_INPUT)).isTrue();
        verifyNoInteractions(catalog);
        verify(trainings, never()).save(any());
    }

    @Test
    void givenInactiveExercise_whenUpdating_thenFailsInactiveExercise() {
        // Given
        when(trainings.findById(TRAINING_ID)).thenReturn(Optional.of(aTraining().withId(TRAINING_ID).build()));
        ExerciseView inactive = new ExerciseView(EXERCISE_ID, "Supino", "CHEST", "d", "Barra", "INTERMEDIATE",
            null, null, "INACTIVE", INSTRUCTOR_ID, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        when(catalog.findById(EXERCISE_ID)).thenReturn(Optional.of(inactive));

        // When
        var result = useCase.execute(command("Treino B", null, INSTRUCTOR_ID, "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_INACTIVE_EXERCISE)).isTrue();
        verify(trainings, never()).save(any());
    }

    @Test
    void givenArchivedTraining_whenUpdating_thenFailsArchived() {
        // Given — an archived training (with otherwise-valid input)
        when(trainings.findById(TRAINING_ID))
            .thenReturn(Optional.of(aTraining().withId(TRAINING_ID).withStatus(TrainingStatus.ARCHIVED).build()));
        when(catalog.findById(EXERCISE_ID)).thenReturn(Optional.of(activeExercise()));

        // When
        var result = useCase.execute(command("Treino B", null, INSTRUCTOR_ID, "INSTRUCTOR"));

        // Then — CONFLICT (409)
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_ARCHIVED)).isTrue();
        verify(trainings, never()).save(any());
    }

    @Test
    void givenOwnerAndValidChanges_whenUpdating_thenPersistsAndPublishesEvent() {
        // Given
        when(trainings.findById(TRAINING_ID)).thenReturn(Optional.of(aTraining().withId(TRAINING_ID).build()));
        when(catalog.findById(EXERCISE_ID)).thenReturn(Optional.of(activeExercise()));
        when(trainings.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(command("Treino B", null, INSTRUCTOR_ID, "INSTRUCTOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().name()).isEqualTo("Treino B");
        verify(trainings).save(any());
        verify(events).publishEvent(any(TrainingUpdated.class));
    }

    @Test
    void givenContraindicatedExercise_whenUpdating_thenFailsContraindicated() {
        // Given — owner, exercise active, but contraindicated for the student (RF-017)
        when(trainings.findById(TRAINING_ID)).thenReturn(Optional.of(aTraining().withId(TRAINING_ID).build()));
        when(catalog.findById(EXERCISE_ID)).thenReturn(Optional.of(activeExercise()));
        when(anamnesis.contraindicatedExercises(STUDENT_ID)).thenReturn(List.of(EXERCISE_ID));

        // When
        var result = useCase.execute(command("Treino B", null, INSTRUCTOR_ID, "INSTRUCTOR"));

        // Then — 422, nothing persisted
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_CONTRAINDICATED_EXERCISE)).isTrue();
        verify(trainings, never()).save(any());
    }
}
