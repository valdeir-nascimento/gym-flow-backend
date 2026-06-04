package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisRepository;
import br.com.gym.flow.users.domain.bond.BondId;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.bond.TeacherStudentBond;
import br.com.gym.flow.users.domain.consent.HealthDataConsentRepository;
import br.com.gym.flow.users.events.AnamnesisRegistered;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterAnamnesisUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INSTRUCTOR = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private AnamnesisRepository anamneses;
    @Mock
    private BondRepository bonds;
    @Mock
    private HealthDataConsentRepository consents;
    @Mock
    private ApplicationEventPublisher events;

    private RegisterAnamnesisUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterAnamnesisUseCase(anamneses, bonds, consents, events, CLOCK);
    }

    private static Notification failureOf(final Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static TeacherStudentBond activeBond() {
        return TeacherStudentBond.hydrate(BondId.newId(), UserId.of(STUDENT), UserId.of(INSTRUCTOR),
            Instant.parse("2026-01-01T00:00:00Z"), null, UserId.of(INSTRUCTOR));
    }

    private static RegisterAnamnesisCommand command(final BigDecimal weight, final int height,
                                                    final UUID actorId, final String actorRole) {
        return new RegisterAnamnesisCommand(STUDENT, weight, height, "Hipertrofia", "iniciante",
            List.of("lesão no ombro"), List.of("hipertensão"), List.of(UUID.randomUUID()), "obs", actorId, actorRole);
    }

    private static RegisterAnamnesisCommand validCommand(final UUID actorId, final String actorRole) {
        return command(new BigDecimal("80.00"), 175, actorId, actorRole);
    }

    @Test
    void givenInstructorNotBondedToStudent_whenRegistering_thenFailsNotManaged() {
        // Given — no active bond
        when(bonds.findActiveByStudent(UserId.of(STUDENT))).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(validCommand(INSTRUCTOR, "INSTRUCTOR"));

        // Then — 403; consent and persistence are never touched
        assertThat(failureOf(result).hasAnyCode(ErrorCode.STUDENT_NOT_MANAGED)).isTrue();
        verifyNoInteractions(consents, anamneses, events);
    }

    @Test
    void givenBondedInstructorButNoConsent_whenRegistering_thenFailsLgpdConsentRequired() {
        // Given — bonded, but the student has not consented
        when(bonds.findActiveByStudent(UserId.of(STUDENT))).thenReturn(Optional.of(activeBond()));
        when(consents.existsByStudentId(STUDENT)).thenReturn(false);

        // When
        var result = useCase.execute(validCommand(INSTRUCTOR, "INSTRUCTOR"));

        // Then — 403 LGPD
        assertThat(failureOf(result).hasAnyCode(ErrorCode.LGPD_CONSENT_REQUIRED)).isTrue();
        verify(anamneses, never()).save(any());
    }

    @Test
    void givenConsentButImplausibleWeight_whenRegistering_thenFailsValidation() {
        // Given — bonded + consent, but weight out of range
        when(bonds.findActiveByStudent(UserId.of(STUDENT))).thenReturn(Optional.of(activeBond()));
        when(consents.existsByStudentId(STUDENT)).thenReturn(true);

        // When
        var result = useCase.execute(command(new BigDecimal("5"), 175, INSTRUCTOR, "INSTRUCTOR"));

        // Then — 422
        assertThat(failureOf(result).hasAnyCode(ErrorCode.ANAMNESIS_IMPLAUSIBLE_WEIGHT)).isTrue();
        verify(anamneses, never()).save(any());
    }

    @Test
    void givenBondedInstructorWithConsentAndValidData_whenRegistering_thenPersistsNextVersionAndPublishes() {
        // Given — already two revisions -> next is version 3
        when(bonds.findActiveByStudent(UserId.of(STUDENT))).thenReturn(Optional.of(activeBond()));
        when(consents.existsByStudentId(STUDENT)).thenReturn(true);
        when(anamneses.latestVersion(STUDENT)).thenReturn(2);
        when(anamneses.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(validCommand(INSTRUCTOR, "INSTRUCTOR"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().studentId()).isEqualTo(STUDENT);
        assertThat(result.getOrThrow().version()).isEqualTo(3);
        verify(anamneses).save(any());
        verify(events).publishEvent(any(AnamnesisRegistered.class));
    }

    @Test
    void givenAdministrator_whenRegistering_thenBypassesBondCheck() {
        // Given — Administrator acts without a bond
        when(consents.existsByStudentId(STUDENT)).thenReturn(true);
        when(anamneses.latestVersion(STUDENT)).thenReturn(0);
        when(anamneses.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(validCommand(UUID.randomUUID(), "ADMINISTRATOR"));

        // Then — succeeds (version 1), bond repository never consulted
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().version()).isEqualTo(1);
        verifyNoInteractions(bonds);
    }
}
