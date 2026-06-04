package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.service.AnamnesisViewMapper;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisDraft;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisRepository;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisRevision;
import br.com.gym.flow.users.domain.anamnesis.AnamnesisValidator;
import br.com.gym.flow.users.domain.bond.BondRepository;
import br.com.gym.flow.users.domain.consent.HealthDataConsentRepository;
import br.com.gym.flow.users.domain.spi.AnamnesisView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RegisterAnamnesisUseCase implements CommandUseCase<RegisterAnamnesisCommand, AnamnesisView> {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    private final AnamnesisRepository anamneses;
    private final BondRepository bonds;
    private final HealthDataConsentRepository consents;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional
    public Result<AnamnesisView> execute(final RegisterAnamnesisCommand command) {
        // Authorization (RNF-001, RF-016): the bonded instructor or an Administrator (403).
        if (!ADMINISTRATOR.equals(command.actorRole()) && !isBonded(command.studentId(), command.actorId())) {
            return Result.failWith(ErrorCode.STUDENT_NOT_MANAGED);
        }

        // Health-data consent must exist beforehand (RNF-008/LGPD -> 403).
        if (!consents.existsByStudentId(command.studentId())) {
            return Result.failWith(ErrorCode.LGPD_CONSENT_REQUIRED);
        }

        // Plausible anthropometric ranges (-> 422).
        return AnamnesisValidator.validate(
                command.weightKg(), command.heightCm(), command.objectives(), command.conditioningHistory(),
                command.injuries(), command.medicalRestrictions(), command.contraindications(), command.observations())
            .map(draft -> registerRevision(draft, command));
    }

    private AnamnesisView registerRevision(final AnamnesisDraft draft, final RegisterAnamnesisCommand command) {
        int nextVersion = anamneses.latestVersion(command.studentId()) + 1;
        AnamnesisRevision revision = AnamnesisRevision.register(
            command.studentId(), nextVersion, draft, command.actorId(), clock);
        AnamnesisRevision saved = anamneses.save(revision);
        revision.pullDomainEvents().forEach(events::publishEvent);
        return AnamnesisViewMapper.toView(saved);
    }

    private boolean isBonded(final java.util.UUID studentId, final java.util.UUID instructorId) {
        return bonds.findActiveByStudent(UserId.of(studentId))
            .map(bond -> bond.instructorId().equals(UserId.of(instructorId)))
            .orElse(false);
    }
}
