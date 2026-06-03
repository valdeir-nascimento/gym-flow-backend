package br.com.gym.flow.users.domain;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED,
    PENDING_FIRST_ACCESS;

    public boolean canAuthenticate() {
        return this == ACTIVE;
    }

    public boolean canTransitionTo(UserStatus target) {
        return switch (this) {
            case PENDING_FIRST_ACCESS -> target == ACTIVE || target == INACTIVE;
            case ACTIVE -> target == INACTIVE || target == BLOCKED;
            case BLOCKED -> target == ACTIVE || target == INACTIVE;
            case INACTIVE -> target == ACTIVE;
        };
    }
}
