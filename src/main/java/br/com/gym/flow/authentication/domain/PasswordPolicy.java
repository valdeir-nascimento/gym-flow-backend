package br.com.gym.flow.authentication.domain;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;

public final class PasswordPolicy {

    private PasswordPolicy() {}

    public static final int MIN_LENGTH = 10;

    public static Result<Void> validate(String raw, PwnedPasswordChecker pwned) {
        if (raw == null || raw.length() < MIN_LENGTH) {
            return Result.failWith(ErrorCode.WEAK_PASSWORD);
        }
        if (classesPresent(raw) < 3) {
            return Result.failWith(ErrorCode.WEAK_PASSWORD,
                "use ao menos 3 dos 4: maiúscula, minúscula, dígito, símbolo");
        }
        if (pwned != null && pwned.isPwned(raw)) {
            return Result.failWith(ErrorCode.PASSWORD_PWNED);
        }
        return Result.ok();
    }

    private static int classesPresent(String raw) {
        boolean upper = false, lower = false, digit = false, symbol = false;
        for (char c : raw.toCharArray()) {
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else symbol = true;
        }
        int n = 0;
        if (upper) n++;
        if (lower) n++;
        if (digit) n++;
        if (symbol) n++;
        return n;
    }
}
