package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.domain.Training;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static br.com.gym.flow.trainings.domain.TrainingTestBuilder.STUDENT_ID;
import static br.com.gym.flow.trainings.domain.TrainingTestBuilder.aTraining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListStudentTrainingsUseCaseTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private TrainingRepository repository;

    private ListStudentTrainingsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListStudentTrainingsUseCase(repository);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenStudentRequestingOwnTrainings_whenListing_thenReturnsMappedPage() {
        // Given
        Training training = aTraining().build(); // studentId = STUDENT_ID
        Page<Training> page = new PageImpl<>(List.of(training), PAGEABLE, 1);
        when(repository.findByStudentId(STUDENT_ID, PAGEABLE)).thenReturn(page);

        // When — the actor is the student themselves
        var result = useCase.execute(new ListStudentTrainingsQuery(STUDENT_ID, STUDENT_ID, "STUDENT", PAGEABLE));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getTotalElements()).isEqualTo(1);
        assertThat(result.getOrThrow().getContent()).singleElement()
            .satisfies(view -> {
                assertThat(view.studentId()).isEqualTo(STUDENT_ID);
                assertThat(view.status()).isEqualTo("ACTIVE");
            });
    }

    @Test
    void givenStudentRequestingAnotherStudentsTrainings_whenListing_thenFailsForbidden() {
        // Given — actor differs from the requested student
        UUID otherStudent = UUID.randomUUID();

        // When
        var result = useCase.execute(new ListStudentTrainingsQuery(otherStudent, STUDENT_ID, "STUDENT", PAGEABLE));

        // Then — ownership policy (403); the repository is never touched
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_NOT_STUDENT_OWNER)).isTrue();
        verifyNoInteractions(repository);
    }

    @Test
    void givenAdministrator_whenListingAnyStudentsTrainings_thenAllowed() {
        // Given — Administrator bypasses ownership
        UUID someStudent = UUID.randomUUID();
        when(repository.findByStudentId(eq(someStudent), any())).thenReturn(Page.empty());

        // When
        var result = useCase.execute(
            new ListStudentTrainingsQuery(someStudent, UUID.randomUUID(), "ADMINISTRATOR", PAGEABLE));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getTotalElements()).isZero();
    }

    @Test
    void givenStudentWithoutTrainings_whenListing_thenReturnsEmptyPage() {
        // Given
        when(repository.findByStudentId(STUDENT_ID, PAGEABLE)).thenReturn(Page.empty(PAGEABLE));

        // When
        var result = useCase.execute(new ListStudentTrainingsQuery(STUDENT_ID, STUDENT_ID, "STUDENT", PAGEABLE));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().getContent()).isEmpty();
        verify(repository).findByStudentId(STUDENT_ID, PAGEABLE);
        verify(repository, never()).save(any());
    }
}
