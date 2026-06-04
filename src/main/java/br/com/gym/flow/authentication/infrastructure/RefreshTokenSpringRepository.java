package br.com.gym.flow.authentication.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface RefreshTokenSpringRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revokedAt = :now WHERE t.userId = :userId AND t.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE RefreshTokenJpaEntity t SET t.revokedAt = :now
        WHERE t.userId = :userId AND t.revokedAt IS NULL AND t.tokenHash <> :keepHash
        """)
    int revokeAllByUserIdExcept(@Param("userId") UUID userId,
                                @Param("keepHash") String keepHash,
                                @Param("now") Instant now);
}
