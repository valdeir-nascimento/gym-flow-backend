package br.com.gym.flow.exercises.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exercises")
class ExerciseJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "name", nullable = false, length = 150)
    String name;

    @Column(name = "muscle_group", nullable = false, length = 30)
    String muscleGroup;

    @Column(name = "description", length = 2000)
    String description;

    @Column(name = "equipment", length = 100)
    String equipment;

    @Column(name = "difficulty_level", nullable = false, length = 20)
    String difficultyLevel;

    @Column(name = "video_url", length = 500)
    String videoUrl;

    @Column(name = "image_url", length = 500)
    String imageUrl;

    @Column(name = "status", nullable = false, length = 20)
    String status;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    Long version;

    ExerciseJpaEntity() {}
}
