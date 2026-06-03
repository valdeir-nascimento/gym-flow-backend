package br.com.gym.flow.users.domain;

public record UserFilter(Role role, UserStatus status, String search) {
    public static UserFilter empty() {
        return new UserFilter(null, null, null);
    }
}
