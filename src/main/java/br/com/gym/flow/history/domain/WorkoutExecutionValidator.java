package br.com.gym.flow.history.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;

import java.time.Instant;
import java.util.List;

/**
 * Structural validation of a workout execution's raw input — timestamps present
 * and the executed exercises' numbers — accumulating every violation into a
 * single {@link Notification}. Temporal business rules (no future timestamps,
 * end ≥ start) and cross-aggregate checks (training ownership, activity window,
 * idempotency) are the aggregate's and use case's job, not this validator's.
 */
public final class WorkoutExecutionValidator {

    private WorkoutExecutionValidator() {
    }

    public static Result<WorkoutExecutionDraft> validate(Instant startedAt, Instant finishedAt, String notes, List<ExecutedExercise> items) {
        Notification notification = Notification.empty();

        if (startedAt == null) {
            notification.addError("startedAt", ErrorCode.INVALID_INPUT);
        }
        if (finishedAt == null) {
            notification.addError("finishedAt", ErrorCode.INVALID_INPUT);
        }
        if (items == null || items.isEmpty()) {
            notification.addError("items", ErrorCode.INVALID_INPUT); // RF-007: at least one performed exercise
        } else {
            validateItems(items, notification);
        }

        return notification.hasErrors()
            ? Result.failure(notification)
            : Result.success(new WorkoutExecutionDraft(startedAt, finishedAt, blankToNull(notes), List.copyOf(items)));
    }

    private static void validateItems(final List<ExecutedExercise> items, final Notification notification) {
        for (int i = 0; i < items.size(); i++) {
            ExecutedExercise item = items.get(i);
            String prefix = "items[" + i + "]";
            if (item.exerciseId() == null) {
                notification.addError(prefix + ".exerciseId", ErrorCode.INVALID_INPUT);
            }
            if (item.sets() <= 0) {
                notification.addError(prefix + ".sets", ErrorCode.INVALID_INPUT);
            }
            if (item.repetitions() <= 0) {
                notification.addError(prefix + ".repetitions", ErrorCode.INVALID_INPUT);
            }
            if (item.load() != null && item.load().signum() < 0) {
                notification.addError(prefix + ".load", ErrorCode.INVALID_INPUT);
            }
        }
    }

    private static String blankToNull(final String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }
}
