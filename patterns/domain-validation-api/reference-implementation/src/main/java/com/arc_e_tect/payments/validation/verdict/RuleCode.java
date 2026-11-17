package com.arc_e_tect.payments.validation.verdict;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The stable, machine-readable code of a rule, such as {@code NAME_IBAN_MISMATCH}:
 * upper-case words joined by single underscores.
 */
public record RuleCode(String value) {

    private static final Pattern UPPER_SNAKE_CASE = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");

    public RuleCode {
        Objects.requireNonNull(value, "value");
        if (!UPPER_SNAKE_CASE.matcher(value).matches()) {
            throw new IllegalArgumentException("A rule code is upper-case words joined by single underscores: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
