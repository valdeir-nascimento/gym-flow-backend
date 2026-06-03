package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.bond.BondId;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.bond.TeacherStudentBond;
import br.com.gym.flow.users.events.BondEnded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveBondUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final BondId BOND_ID = BondId.of(UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
    private static final UserId STUDENT_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final UserId INSTRUCTOR_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final UserId OUTSIDER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000009"));

    @Mock
    private BondRepository bonds;
    @Mock
    private ApplicationEventPublisher events;

    private RemoveBondUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RemoveBondUseCase(bonds, events, CLOCK);
    }

    private static Notification failureOf(Result<?> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?>) result).notification();
    }

    private static TeacherStudentBond activeBond() {
        return TeacherStudentBond.hydrate(
            BOND_ID, STUDENT_ID, INSTRUCTOR_ID, Instant.parse("2026-01-01T00:00:00Z"), null, INSTRUCTOR_ID);
    }

    private static TeacherStudentBond endedBond() {
        return TeacherStudentBond.hydrate(
            BOND_ID, STUDENT_ID, INSTRUCTOR_ID,
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z"), INSTRUCTOR_ID);
    }

    @Test
    void givenUnknownBond_whenRemoving_thenFailsNotFound() {
        // Given
        when(bonds.findById(BOND_ID)).thenReturn(Optional.empty());

        // When
        var result = useCase.execute(new RemoveBondCommand(BOND_ID, INSTRUCTOR_ID, false));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.BOND_NOT_FOUND)).isTrue();
        verify(bonds, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenAlreadyEndedBond_whenRemoving_thenFailsNotFound() {
        // Given
        when(bonds.findById(BOND_ID)).thenReturn(Optional.of(endedBond()));

        // When
        var result = useCase.execute(new RemoveBondCommand(BOND_ID, INSTRUCTOR_ID, false));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.BOND_NOT_FOUND)).isTrue();
        verify(bonds, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenInstructorRestrictionAndForeignActor_whenRemoving_thenFailsNotOwned() {
        // Given — restricted to the owning instructor, but the actor is someone else
        when(bonds.findById(BOND_ID)).thenReturn(Optional.of(activeBond()));

        // When
        var result = useCase.execute(new RemoveBondCommand(BOND_ID, OUTSIDER_ID, true));

        // Then
        assertThat(failureOf(result).hasAnyCode(ErrorCode.BOND_NOT_OWNED_BY_INSTRUCTOR)).isTrue();
        verify(bonds, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void givenActiveBondAndUnrestrictedActor_whenRemoving_thenClosesPersistsAndPublishes() {
        // Given
        when(bonds.findById(BOND_ID)).thenReturn(Optional.of(activeBond()));
        when(bonds.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(new RemoveBondCommand(BOND_ID, OUTSIDER_ID, false));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrThrow().active()).isFalse();
        verify(bonds).save(any(TeacherStudentBond.class));
        verify(events).publishEvent(any(BondEnded.class));
    }

    @Test
    void givenInstructorRestrictionAndOwningActor_whenRemoving_thenSucceeds() {
        // Given — the actor is the owning instructor, so the restriction passes
        when(bonds.findById(BOND_ID)).thenReturn(Optional.of(activeBond()));
        when(bonds.save(any())).then(returnsFirstArg());

        // When
        var result = useCase.execute(new RemoveBondCommand(BOND_ID, INSTRUCTOR_ID, true));

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(events).publishEvent(any(BondEnded.class));
    }
}
