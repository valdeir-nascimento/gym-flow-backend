package br.com.gym.flow.users.application.usecase;

import java.util.UUID;

/**
 * Records a student's health-data consent (RF-017 / RNF-008). Granted by the
 * student themselves or by an Administrator.
 */
public record GrantHealthConsentCommand(UUID studentId, UUID actorId, String actorRole) {}
