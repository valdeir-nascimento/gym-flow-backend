package br.com.gym.flow.trainings.domain.spi;

import java.math.BigDecimal;
import java.util.UUID;

public record TrainingItemView(
    UUID exerciseId,
    int sets,
    int repetitions,
    BigDecimal load,
    int restSeconds
) {}
