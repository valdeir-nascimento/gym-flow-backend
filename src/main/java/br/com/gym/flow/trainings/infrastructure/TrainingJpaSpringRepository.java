package br.com.gym.flow.trainings.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

interface TrainingJpaSpringRepository extends JpaRepository<TrainingJpaEntity, UUID> {

    /**
     * True when the student already has an ACTIVE training overlapping
     * [newStart, newEnd] (a null end means open-ended on either side).
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM TrainingJpaEntity t
        WHERE t.studentId = :studentId
          AND t.status = 'ACTIVE'
          AND (:newEnd IS NULL OR t.startDate <= :newEnd)
          AND (t.endDate IS NULL OR t.endDate >= :newStart)
        """)
    boolean existsActiveOverlapping(@Param("studentId") UUID studentId,
                                    @Param("newStart") LocalDate newStart,
                                    @Param("newEnd") LocalDate newEnd);
}
