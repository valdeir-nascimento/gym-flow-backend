package br.com.gym.flow.shared.domain;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID id();
    Instant occurredOn();
}
