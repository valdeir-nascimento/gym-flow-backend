package br.com.gym.flow.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SharedClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
