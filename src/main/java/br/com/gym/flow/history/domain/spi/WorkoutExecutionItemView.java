package br.com.gym.flow.history.domain.spi;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkoutExecutionItemView(
    UUID exerciseId,
    int sets,
    int repetitions,
    BigDecimal load,
    String notes
) {}
