package br.com.gym.flow.shared.domain;

import java.util.Objects;

public record PostalCode(String value) {

    public PostalCode {
        Objects.requireNonNull(value, "value");
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new IllegalArgumentException("invalid CEP: " + value);
        }
        value = digits;
    }

    public static PostalCode of(String raw) {
        return new PostalCode(raw);
    }

    public String formatted() {
        return value.substring(0, 5) + "-" + value.substring(5);
    }
}
