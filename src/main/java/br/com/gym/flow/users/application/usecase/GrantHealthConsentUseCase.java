package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.service.AnamnesisViewMapper;
import br.com.gym.flow.users.domain.consent.HealthDataConsent;
import br.com.gym.flow.users.domain.consent.HealthDataConsentRepository;
import br.com.gym.flow.users.domain.spi.HealthConsentView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class GrantHealthConsentUseCase implements CommandUseCase<GrantHealthConsentCommand, HealthConsentView> {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    private final HealthDataConsentRepository consents;
    private final Clock clock;

    @Override
    @Transactional
    public Result<HealthConsentView> execute(final GrantHealthConsentCommand command) {
        // Consent is the student's to give (or an Administrator on their behalf).
        if (!ADMINISTRATOR.equals(command.actorRole()) && !command.actorId().equals(command.studentId())) {
            return Result.failWith(ErrorCode.FORBIDDEN_ROLE);
        }

        // Idempotent: an existing consent is returned as-is.
        return consents.findByStudentId(command.studentId())
            .map(existing -> Result.success(AnamnesisViewMapper.toView(existing)))
            .orElseGet(() -> {
                HealthDataConsent granted = consents.save(
                    HealthDataConsent.grant(command.studentId(), command.actorId(), clock));
                return Result.success(AnamnesisViewMapper.toView(granted));
            });
    }
}
