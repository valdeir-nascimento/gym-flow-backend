package br.com.gym.flow.exercises.domain.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model of an exercise exposed at the API boundary and to other modules
 * (e.g. {@code treinos}). Enums are carried as their {@code name()} string so
 * consumers do not depend on this module's enum types.
 */
public record ExerciseView(
    UUID id,
    String name,
    String muscleGroup,
    String description,
    String equipment,
    String difficultyLevel,
    String videoUrl,
    String imageUrl,
    String status,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
