package br.com.gym.flow.shared.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record PhoneNumber(String value) {

    private static final Pattern E164 = Pattern.compile("^\\+55\\d{10,11}$");

    public PhoneNumber {
        Objects.requireNonNull(value, "value");
        String digits = value.replaceAll("[^\\d+]", "");
        if (!digits.startsWith("+")) {
            digits = "+55" + digits;
        }
        if (!E164.matcher(digits).matches()) {
            throw new IllegalArgumentException("invalid phone number: " + value);
        }
        value = digits;
    }

    public static PhoneNumber of(String raw) {
        return new PhoneNumber(raw);
    }

    public String formatted() {
        // +55 (11) 91234-5678  or  +55 (11) 3123-4567
        String areaCode = value.substring(3, 5);
        String rest = value.substring(5);
        if (rest.length() == 9) {
            return "+55 (%s) %s-%s".formatted(areaCode, rest.substring(0, 5), rest.substring(5));
        }
        return "+55 (%s) %s-%s".formatted(areaCode, rest.substring(0, 4), rest.substring(4));
    }
}
