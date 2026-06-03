package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.domain.TrainingId;
import br.com.gym.flow.trainings.domain.TrainingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static br.com.gym.flow.trainings.domain.TrainingTestBuilder.aTraining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTrainingUseCaseTest {

    private static final TrainingId TRAINING_ID = TrainingId.of(UUID.fromString("00000000-0000-0000-0000-000000000010"));

    @Mock
    private TrainingRepository repository;

    private GetTrainingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTrainingUseCase(repository);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenExistingTraining_whenGetting_thenReturnsItsView() {
        // Given
        when(repository.findById(TRAINING_ID)).thenReturn(Optional.of(aTraining().withId(TRAINING_ID).build()));

        // When
        var result = useCase.execute(new GetTrainingQuery(TRAINING_ID));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().id()).isEqualTo(TRAINING_ID.value());
        assertThat(result.getOrThrow().items()).hasSize(1);
    }

    @Test
    void givenUnknownTraining_whenGetting_thenFailsNotFound() {
        // Given
        when(repository.findById(TRAINING_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new GetTrainingQuery(TRAINING_ID));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.TRAINING_NOT_FOUND)).isTrue();
    }
}
