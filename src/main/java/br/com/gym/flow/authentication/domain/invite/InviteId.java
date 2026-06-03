package br.com.gym.flow.authentication.domain.invite;

import java.util.Objects;
import java.util.UUID;

public record InviteId(UUID value) {
    public InviteId { Objects.requireNonNull(value, "value"); }
    public static InviteId newId() { return new InviteId(UUID.randomUUID()); }
    public static InviteId of(UUID v) { return new InviteId(v); }
}
