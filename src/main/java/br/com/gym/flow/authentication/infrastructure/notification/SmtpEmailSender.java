package br.com.gym.flow.authentication.infrastructure.notification;

import br.com.gym.flow.authentication.application.config.MailProperties;
import br.com.gym.flow.authentication.domain.notification.EmailMessage;
import br.com.gym.flow.authentication.domain.notification.EmailSender;
import br.com.gym.flow.shared.domain.DomainException;
import br.com.gym.flow.shared.domain.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailProperties properties;

    @Override
    public void send(EmailMessage message) {
        try {
            Context ctx = new Context();
            ctx.setVariables(message.variables());
            String html = templateEngine.process(message.templateName(), ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from(), properties.senderName());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(html, true);

            mailSender.send(mime);
            log.info("email dispatched to={} template={}", message.to(), message.templateName());
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("failed to send email to {}: {}", message.to(), ex.getMessage());
            throw new MailDeliveryException();
        }
    }

    static final class MailDeliveryException extends DomainException {
        MailDeliveryException() {
            super(ErrorCode.MAIL_DELIVERY_FAILED.name(), ErrorCode.MAIL_DELIVERY_FAILED.defaultMessage());
        }
    }
}
