package br.com.gym.flow.trainings.domain;

import java.util.List;

/**
 * Already-validated attributes of a training, handed to {@link Training#create}.
 * Built by {@link TrainingValidator}; {@code items} is non-empty and each item
 * is structurally valid. Catalog/availability checks (exercise active, etc.) are
 * the use case's responsibility, not this carrier's.
 */
public record TrainingDraft(String name, String objective, TrainingPeriod period, List<TrainingItem> items) {}
