package br.com.gym.flow.authentication.application.usecase;

import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.authentication.domain.notification.EmailMessage;
import br.com.gym.flow.authentication.domain.notification.EmailSender;
import br.com.gym.flow.authentication.domain.recovery.PasswordResetToken;
import br.com.gym.flow.authentication.domain.recovery.PasswordResetTokenRepository;
import br.com.gym.flow.shared.application.CommandUseCase;
import br.com.gym.flow.shared.domain.Email;
import br.com.gym.flow.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import br.com.gym.flow.authentication.application.config.MailProperties;
import br.com.gym.flow.authentication.application.config.PasswordRecoveryProperties;


@Slf4j
@Service
@RequiredArgsConstructor
public class RequestPasswordRecoveryUseCase implements CommandUseCase<RecoveryRequestCommand, Void> {

    private final UserCredentialsRepository credentials;
    private final PasswordResetTokenRepository tokens;
    private final EmailSender emailSender;
    private final PasswordRecoveryProperties recovery;
    private final MailProperties mailProps;
    private final Clock clock;

    @Override
    @Transactional
    public Result<Void> execute(RecoveryRequestCommand command) {
        // Always succeed (HTTP 202) to avoid email enumeration.
        String normalizedEmail;
        try {
            normalizedEmail = Email.of(command.rawEmail()).value();
        } catch (IllegalArgumentException ex) {
            return Result.ok();
        }

        Optional<UserCredentials> maybe = credentials.findByEmail(normalizedEmail);
        if (maybe.isEmpty()) return Result.ok();
        UserCredentials cred = maybe.get();
        if (!cred.status().isActive() && !cred.status().isBlocked()) {
            return Result.ok();
        }

        PasswordResetToken.Issued issued = PasswordResetToken.issue(cred.userId(), recovery.ttl(), clock);
        tokens.save(issued.token());

        String resetUrl = "%s/api/v1/auth/password-reset/%s".formatted(
            mailProps.baseUrl(), issued.rawToken());

        try {
            emailSender.send(new EmailMessage(
                cred.email(),
                "Redefinição de senha — WS Fitness",
                "email/password-recovery",
                Map.of("resetUrl", resetUrl)
            ));
        } catch (RuntimeException ex) {
            log.warn("recovery email delivery failed for {}: {}", cred.email(), ex.getMessage());
        }
        return Result.ok();
    }
}
