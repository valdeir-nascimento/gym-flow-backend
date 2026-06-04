package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.consent.HealthDataConsent;
import br.com.gym.flow.users.domain.consent.HealthDataConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrantHealthConsentUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private HealthDataConsentRepository consents;

    private GrantHealthConsentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GrantHealthConsentUseCase(consents, CLOCK);
    }

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    @Test
    void givenStudentConsentingForThemselves_whenGranting_thenPersistsConsent() {
        // Given
        when(consents.findByStudentId(STUDENT)).thenReturn(Optional.empty());
        when(consents.save(any())).then(returnsFirstArg());

        // When — the student is the actor
        var result = useCase.execute(new GrantHealthConsentCommand(STUDENT, STUDENT, "STUDENT"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().studentId()).isEqualTo(STUDENT);
        verify(consents).save(any());
    }

    @Test
    void givenInstructorActing_whenGranting_thenFailsForbiddenRole() {
        // When — a non-admin, non-self actor
        var result = useCase.execute(new GrantHealthConsentCommand(STUDENT, UUID.randomUUID(), "INSTRUCTOR"));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.FORBIDDEN_ROLE)).isTrue();
        verify(consents, never()).save(any());
    }

    @Test
    void givenExistingConsent_whenGranting_thenReturnsItIdempotently() {
        // Given — Administrator re-grants for a student who already consented
        HealthDataConsent existing = HealthDataConsent.grant(STUDENT, STUDENT, CLOCK);
        when(consents.findByStudentId(STUDENT)).thenReturn(Optional.of(existing));

        // When
        var result = useCase.execute(new GrantHealthConsentCommand(STUDENT, UUID.randomUUID(), "ADMINISTRATOR"));

        // Then — returns the existing consent, no new row
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().id()).isEqualTo(existing.id());
        verify(consents, never()).save(any());
    }
}
