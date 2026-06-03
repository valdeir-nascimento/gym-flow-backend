package br.com.gym.flow.users.infrastructure;

import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.User;
import br.com.gym.flow.users.domain.UserRepository;
import br.com.gym.flow.users.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static br.com.gym.flow.users.domain.UserTestBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence slice for the {@code users} context. Runs against a real Postgres
 * (Testcontainers) with the production Flyway migrations applied and
 * {@code ddl-auto=validate} (profile {@code test}) — so the boot itself fails if an
 * entity diverges from the migrated schema. Requires Docker available on the host.
 *
 * <p>Proves two things the unit/slice tests cannot: that the hand-written
 * {@link UserJpaMapper} round-trips a {@link User} faithfully, and that the
 * {@code @Version} column added in V9 actually enforces optimistic locking.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@Import(UserRepositoryAdapter.class)
class UserPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository repository;
    @Autowired
    private UserJpaSpringRepository jpa;
    @Autowired
    private TestEntityManager em;

    private static UserJpaEntity newEntity(UUID id) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = id;
        entity.name = "Maria Silva";
        entity.email = "maria+" + id + "@example.com";
        entity.phone = "+5511912345678";
        entity.birthDate = LocalDate.of(2000, 1, 1);
        entity.role = Role.STUDENT.name();
        entity.status = UserStatus.ACTIVE.name();
        entity.createdAt = Instant.parse("2026-01-01T00:00:00Z");
        entity.updatedAt = Instant.parse("2026-01-01T00:00:00Z");
        return entity;
    }

    @Test
    void givenSavedUser_whenReloading_thenMapperRoundTripsAllFields() {
        // Given — a domain user persisted through the adapter (domain -> entity mapping)
        User saved = repository.save(aUser().build());

        // When — reload it (entity -> domain mapping)
        User reloaded = repository.findById(saved.id()).orElseThrow();

        // Then — every field survived the round trip
        assertThat(reloaded.id()).isEqualTo(saved.id());
        assertThat(reloaded.name()).isEqualTo("Maria Silva");
        assertThat(reloaded.email().value()).isEqualTo("maria@example.com");
        assertThat(reloaded.phone().value()).isEqualTo("+5511912345678");
        assertThat(reloaded.role()).isEqualTo(Role.STUDENT);
        assertThat(reloaded.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(reloaded.birthDate().value()).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    void givenStaleVersion_whenSaving_thenOptimisticLockingRejectsTheUpdate() {
        // Given — a persisted row loaded into a now-detached, version-0 copy
        UUID id = UUID.randomUUID();
        em.persistAndFlush(newEntity(id));
        em.clear();
        UserJpaEntity stale = jpa.findById(id).orElseThrow();
        em.clear();

        // And — a concurrent transaction has since bumped the row's version
        em.getEntityManager()
            .createNativeQuery("UPDATE users SET version = version + 1, name = 'changed by other' WHERE id = :id")
            .setParameter("id", id)
            .executeUpdate();
        em.flush();
        em.clear();

        // When / Then — saving the stale copy fails the version check instead of clobbering
        stale.name = "changed by me";
        assertThatThrownBy(() -> jpa.saveAndFlush(stale))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
