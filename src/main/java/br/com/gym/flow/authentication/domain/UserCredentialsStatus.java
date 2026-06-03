package br.com.gym.flow.authentication.domain;

public enum UserCredentialsStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED,
    PENDING_FIRST_ACCESS;

    public boolean isActive() { return this == ACTIVE; }
    public boolean isBlocked() { return this == BLOCKED; }
}
