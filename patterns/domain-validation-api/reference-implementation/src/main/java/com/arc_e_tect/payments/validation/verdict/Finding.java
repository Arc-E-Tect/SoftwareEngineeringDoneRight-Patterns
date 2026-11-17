package com.arc_e_tect.payments.validation.verdict;

import java.util.Objects;
import java.util.Optional;

/**
 * One thing a validation found about its input: which rule, how much it matters, a message for
 * people, and, where it applies, a JSON Pointer (RFC 6901) to the part of the input it is about.
 */
public record Finding(RuleCode rule, Severity severity, String message, Optional<String> pointer) {

    public Finding {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(pointer, "pointer");
        if (message.isBlank()) {
            throw new IllegalArgumentException("A finding needs a message");
        }
        pointer.filter(value -> !value.startsWith("/")).ifPresent(value -> {
            throw new IllegalArgumentException("A pointer into the input starts with '/': " + value);
        });
    }

    /** A finding about the input as a whole. */
    public static Finding of(RuleCode rule, Severity severity, String message) {
        return new Finding(rule, severity, message, Optional.empty());
    }

    /** The same finding, about the part of the input the pointer refers to. */
    public Finding at(String pointer) {
        return new Finding(rule, severity, message, Optional.of(pointer));
    }
}
