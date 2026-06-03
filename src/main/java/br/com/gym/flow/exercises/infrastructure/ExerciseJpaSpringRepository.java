package br.com.gym.flow.exercises.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface ExerciseJpaSpringRepository extends JpaRepository<ExerciseJpaEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    @Query("""
        SELECT e FROM ExerciseJpaEntity e
        WHERE (:muscleGroup IS NULL OR e.muscleGroup = :muscleGroup)
          AND (:difficultyLevel IS NULL OR e.difficultyLevel = :difficultyLevel)
          AND (:status IS NULL OR e.status = :status)
          AND (:equipment IS NULL OR LOWER(e.equipment) LIKE LOWER(CONCAT('%', :equipment, '%')))
          AND (:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<ExerciseJpaEntity> search(@Param("muscleGroup") String muscleGroup,
                                   @Param("difficultyLevel") String difficultyLevel,
                                   @Param("equipment") String equipment,
                                   @Param("search") String search,
                                   @Param("status") String status,
                                   Pageable pageable);
}
