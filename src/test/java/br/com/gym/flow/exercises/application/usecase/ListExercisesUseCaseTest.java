package br.com.gym.flow.exercises.application.usecase;

import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.Exercise;
import br.com.gym.flow.exercises.domain.ExerciseFilter;
import br.com.gym.flow.exercises.domain.ExerciseRepository;
import br.com.gym.flow.exercises.domain.ExerciseStatus;
import br.com.gym.flow.exercises.domain.MuscleGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static br.com.gym.flow.exercises.domain.ExerciseTestBuilder.anExercise;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListExercisesUseCaseTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private ExerciseRepository repository;

    private ListExercisesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListExercisesUseCase(repository);
    }

    @Test
    void givenMatchingExercises_whenListing_thenReturnsMappedPage() {
        // Given
        Exercise exercise = anExercise().build();
        Page<Exercise> page = new PageImpl<>(List.of(exercise), PAGEABLE, 1);
        when(repository.search(any(ExerciseFilter.class), eq(PAGEABLE))).thenReturn(page);

        // When
        var result = useCase.execute(new ListExercisesQuery(null, null, null, null, null, PAGEABLE));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getTotalElements()).isEqualTo(1);
        assertThat(result.getOrThrow().getContent()).singleElement()
            .satisfies(view -> assertThat(view.id()).isEqualTo(exercise.id().value()));
    }

    @Test
    void givenFilterParams_whenListing_thenForwardsThemAsAnExerciseFilter() {
        // Given
        when(repository.search(any(ExerciseFilter.class), any())).thenReturn(Page.empty());

        // When
        useCase.execute(new ListExercisesQuery(
            MuscleGroup.CHEST, DifficultyLevel.BEGINNER, "Barra", "supino", ExerciseStatus.ACTIVE, PAGEABLE));

        // Then — query fields are forwarded verbatim into the domain filter
        var captor = ArgumentCaptor.forClass(ExerciseFilter.class);
        verify(repository).search(captor.capture(), eq(PAGEABLE));
        assertThat(captor.getValue().muscleGroup()).isEqualTo(MuscleGroup.CHEST);
        assertThat(captor.getValue().difficultyLevel()).isEqualTo(DifficultyLevel.BEGINNER);
        assertThat(captor.getValue().equipment()).isEqualTo("Barra");
        assertThat(captor.getValue().search()).isEqualTo("supino");
        assertThat(captor.getValue().status()).isEqualTo(ExerciseStatus.ACTIVE);
    }
}
