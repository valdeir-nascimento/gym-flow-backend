package br.com.gym.flow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-context smoke test: boots the whole application against a real Postgres
 * (Testcontainers), running every Flyway migration with {@code ddl-auto=validate}
 * and wiring all beans — controllers, filters, event listeners, etc. Catches
 * boot-time failures (schema mismatches, invalid @TransactionalEventListener
 * config, missing beans) that the slice/unit tests cannot. Requires Docker.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class GymFlowBackendApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void contextLoads() {
    }

}
