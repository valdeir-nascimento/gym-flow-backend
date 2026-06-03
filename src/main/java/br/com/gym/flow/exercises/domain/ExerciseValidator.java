package br.com.gym.flow.exercises.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;

import java.net.URI;

/**
 * Parses and validates the raw fields of an exercise, accumulating every
 * violation into a single {@link Notification}. On success hands back an
 * {@link ExerciseDetails} with the name trimmed and the optional blank fields
 * normalized to {@code null}. Mirrors the user-registration validator.
 */
public final class ExerciseValidator {

    private ExerciseValidator() {
    }

    public static Result<ExerciseDetails> validate(
        String name,
        MuscleGroup muscleGroup,
        String description,
        String equipment,
        DifficultyLevel difficultyLevel,
        String videoUrl,
        String imageUrl
    ) {
        Notification notification = Notification.empty();

        String parsedName = parseName(name, notification);
        if (muscleGroup == null) {
            notification.addError("muscleGroup", ErrorCode.INVALID_INPUT);
        }
        if (difficultyLevel == null) {
            notification.addError("difficultyLevel", ErrorCode.INVALID_INPUT);
        }
        String parsedVideo = parseOptionalUrl(videoUrl, "videoUrl", notification);
        String parsedImage = parseOptionalUrl(imageUrl, "imageUrl", notification);

        return notification.hasErrors()
            ? Result.failure(notification)
            : Result.success(new ExerciseDetails(
            parsedName, muscleGroup, blankToNull(description), blankToNull(equipment),
            difficultyLevel, parsedVideo, parsedImage));
    }

    private static String parseName(String raw, Notification notification) {
        if (raw == null || raw.isBlank()) {
            notification.addError("name", ErrorCode.INVALID_INPUT);
            return null;
        }
        return raw.trim();
    }

    private static String parseOptionalUrl(String raw, String field, Notification notification) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (!isHttpUrl(trimmed)) {
            notification.addError(field, ErrorCode.INVALID_INPUT);
            return null;
        }
        return trimmed;
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return ("http".equals(scheme) || "https".equals(scheme)) && uri.getHost() != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String blankToNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }
}
