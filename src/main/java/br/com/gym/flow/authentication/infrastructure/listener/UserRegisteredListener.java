package br.com.gym.flow.authentication.infrastructure.listener;

import br.com.gym.flow.authentication.application.config.InviteProperties;
import br.com.gym.flow.authentication.application.config.MailProperties;
import br.com.gym.flow.authentication.domain.UserCredentials;
import br.com.gym.flow.authentication.domain.UserCredentialsRepository;
import br.com.gym.flow.authentication.domain.invite.Invite;
import br.com.gym.flow.authentication.domain.invite.InviteRepository;
import br.com.gym.flow.authentication.domain.notification.EmailMessage;
import br.com.gym.flow.authentication.domain.notification.EmailSender;
import br.com.gym.flow.users.events.UserRegistered;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
class UserRegisteredListener {

    private final UserCredentialsRepository credentials;
    private final InviteRepository invites;
    private final EmailSender emailSender;
    private final InviteProperties inviteProps;
    private final MailProperties mailProps;
    private final Clock clock;

    @Transactional
    @TransactionalEventListener
    public void on(UserRegistered event) {
        UserCredentials pending = UserCredentials.pending(event.userId(), event.email(), event.role());
        credentials.save(pending);

        Invite.IssuedInvite issued = Invite.issue(event.userId(), inviteProps.ttl(), clock);
        invites.save(issued.invite());

        String inviteUrl = "%s/api/v1/auth/invites/%s/consume".formatted(
            mailProps.baseUrl(), issued.rawToken());

        emailSender.send(new EmailMessage(
            event.email(),
            "Bem-vindo ao WS Fitness — defina sua senha",
            "email/invite",
            Map.of(
                "name", event.name(),
                "role", roleLabel(event.role()),
                "inviteUrl", inviteUrl
            )
        ));

        log.info("invite issued for userId={} role={}", event.userId(), event.role());
    }

    private static String roleLabel(String role) {
        return switch (role) {
            case "STUDENT" -> "ALUNO";
            case "INSTRUCTOR" -> "PROFESSOR";
            case "ADMINISTRATOR" -> "ADMINISTRADOR";
            default -> role;
        };
    }
}
