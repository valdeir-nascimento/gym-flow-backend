package br.com.gym.flow.authentication.application.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    InviteProperties.class,
    MailProperties.class,
    LoginThrottleProperties.class,
    PasswordRecoveryProperties.class
})
public class AuthenticationProperties {
}
