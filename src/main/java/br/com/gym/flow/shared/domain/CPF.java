package br.com.gym.flow.shared.domain;

import java.util.Objects;

public record CPF(String value) {

    public CPF {
        Objects.requireNonNull(value, "value");
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 11 || allSameDigit(digits) || !validChecksum(digits)) {
            throw new IllegalArgumentException("invalid CPF: " + value);
        }
        value = digits;
    }

    public static CPF of(String raw) {
        return new CPF(raw);
    }

    public String formatted() {
        return "%s.%s.%s-%s".formatted(
            value.substring(0, 3),
            value.substring(3, 6),
            value.substring(6, 9),
            value.substring(9, 11)
        );
    }

    public String masked() {
        return "***.***.***-" + value.substring(9, 11);
    }

    private static boolean allSameDigit(String d) {
        return d.chars().distinct().count() == 1;
    }

    private static boolean validChecksum(String d) {
        return checkDigit(d, 9) == digit(d, 9) && checkDigit(d, 10) == digit(d, 10);
    }

    private static int checkDigit(String d, int position) {
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += digit(d, i) * (position + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int digit(String d, int idx) {
        return d.charAt(idx) - '0';
    }
}
