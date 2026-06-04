package br.com.gym.flow.users.domain.anamnesis;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Notification;
import br.com.gym.flow.shared.domain.Result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Validates anamnesis input (RF-017): the anthropometric measures must fall in a
 * plausible range (-> 422). Field presence is enforced at the request layer (400);
 * cross-aggregate checks (bond, consent, student active) are the use case's job.
 */
public final class AnamnesisValidator {

    private AnamnesisValidator() {
    }

    public static Result<AnamnesisDraft> validate(
        final BigDecimal weightKg,
        final int heightCm,
        final String objectives,
        final String conditioningHistory,
        final List<String> injuries,
        final List<String> medicalRestrictions,
        final List<UUID> contraindications,
        final String observations
    ) {
        Notification notification = Notification.empty();

        Weight weight = null;
        try {
            weight = Weight.of(weightKg);
        } catch (IllegalArgumentException ex) {
            notification.addError("weightKg", ErrorCode.ANAMNESIS_IMPLAUSIBLE_WEIGHT);
        }

        Height height = null;
        try {
            height = Height.of(heightCm);
        } catch (IllegalArgumentException ex) {
            notification.addError("heightCm", ErrorCode.ANAMNESIS_IMPLAUSIBLE_HEIGHT);
        }

        if (notification.hasErrors()) {
            return Result.failure(notification);
        }
        return Result.success(new AnamnesisDraft(
            weight, height,
            objectives == null ? null : objectives.trim(),
            conditioningHistory == null ? null : conditioningHistory.trim(),
            injuries == null ? List.of() : List.copyOf(injuries),
            medicalRestrictions == null ? List.of() : List.copyOf(medicalRestrictions),
            contraindications == null ? List.of() : List.copyOf(contraindications),
            observations == null ? null : observations.trim()));
    }
}
