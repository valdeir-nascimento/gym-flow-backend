package br.com.gym.flow.trainings.application.usecase;

import br.com.gym.flow.exercises.domain.spi.ExerciseCatalog;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.trainings.domain.TrainingItem;
import br.com.gym.flow.users.domain.spi.AnamnesisDirectory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared catalog validation for training use cases: every referenced exercise
 * must exist (else a 400 validation error) and be active (else 422). Used by
 * both creation (RF-004) and update (RF-005), since an update may add exercises.
 */
final class CatalogChecks {

    private static final String ACTIVE = "ACTIVE";

    private CatalogChecks() {}

    static Result<Void> allExercisesActive(ExerciseCatalog catalog, List<TrainingItem> items) {
        for (TrainingItem item : items) {
            Optional<ExerciseView> exercise = catalog.findById(item.exerciseId());
            if (exercise.isEmpty()) {
                return Result.failWith(ErrorCode.INVALID_INPUT, "exercício inexistente no catálogo: " + item.exerciseId());
            }
            if (!ACTIVE.equals(exercise.get().status())) {
                return Result.failWith(ErrorCode.TRAINING_INACTIVE_EXERCISE);
            }
        }
        return Result.ok();
    }

    /**
     * Rejects items the student must not perform per their latest anamnesis
     * (RF-017 → RF-004/RF-005): a contraindicated exercise cannot compose the
     * training (422). No anamnesis means no contraindications.
     */
    static Result<Void> noContraindicatedExercises(AnamnesisDirectory anamnesis, UUID studentId, List<TrainingItem> items) {
        List<UUID> contraindicated = anamnesis.contraindicatedExercises(studentId);
        if (contraindicated.isEmpty()) {
            return Result.ok();
        }
        for (TrainingItem item : items) {
            if (contraindicated.contains(item.exerciseId())) {
                return Result.failWith(ErrorCode.TRAINING_CONTRAINDICATED_EXERCISE,
                    "exercício contraindicado para o aluno: " + item.exerciseId());
            }
        }
        return Result.ok();
    }
}
