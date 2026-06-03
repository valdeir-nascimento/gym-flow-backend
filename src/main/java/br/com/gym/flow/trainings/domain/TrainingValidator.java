package br.com.gym.flow.trainings.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;

import java.time.LocalDate;
import java.util.List;

/**
 * Structural validation of a training's raw input — name, validity window and
 * the items' prescription numbers — accumulating every violation into a single
 * {@link Notification}. Cross-aggregate checks (student exists/active, exercise
 * active, period overlap) are the use case's job, not this validator's.
 */
public final class TrainingValidator {

    private TrainingValidator() {
    }

    public static Result<TrainingDraft> validate(String name, String objective, LocalDate startDate, LocalDate endDate, List<TrainingItem> items) {
        Notification notification = Notification.empty();

        String parsedName = parseName(name, notification);
        if (startDate == null) {
            notification.addError("startDate", ErrorCode.INVALID_INPUT);
        } else if (endDate != null && endDate.isBefore(startDate)) {
            notification.addError("endDate", ErrorCode.INVALID_INPUT);
        }
        if (items == null || items.isEmpty()) {
            notification.addError("items", ErrorCode.INVALID_INPUT); // RF-004: a training must have ≥1 exercise
        } else {
            validateItems(items, notification);
        }

        return notification.hasErrors()
            ? Result.failure(notification)
            : Result.success(new TrainingDraft(
            parsedName, blankToNull(objective), new TrainingPeriod(startDate, endDate), List.copyOf(items)));
    }

    private static void validateItems(List<TrainingItem> items, Notification notification) {
        for (int i = 0; i < items.size(); i++) {
            TrainingItem item = items.get(i);
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
            if (item.restSeconds() < 0) {
                notification.addError(prefix + ".restSeconds", ErrorCode.INVALID_INPUT);
            }
            if (item.load() != null && item.load().signum() < 0) {
                notification.addError(prefix + ".load", ErrorCode.INVALID_INPUT);
            }
        }
    }

    private static String parseName(String raw, Notification notification) {
        if (raw == null || raw.isBlank()) {
            notification.addError("name", ErrorCode.INVALID_INPUT);
            return null;
        }
        return raw.trim();
    }

    private static String blankToNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }
}
