package br.com.gym.flow.users.application.usecase;

import java.util.UUID;

public record ListAnamnesisHistoryQuery(UUID studentId, UUID actorId, String actorRole) {}
