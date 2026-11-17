package com.arc_e_tect.payments.validation.verdict;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What a validation concluded: its outcome, the details specific to the validation, and its
 * findings.
 *
 * <p>The outcome and the findings are independent, within two limits: a verdict is invalid if,
 * and only if, it has an error finding, and an indeterminate verdict has no details, because no
 * verdict was reached.
 *
 * @param <D> the details of the validation that reached it
 */
public record Verdict<D>(Outcome outcome, Optional<D> details, List<Finding> findings) {

    public Verdict {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(details, "details");
        findings = List.copyOf(findings);
        boolean hasError = findings.stream().anyMatch(finding -> finding.severity() == Severity.ERROR);
        if (hasError != (outcome == Outcome.INVALID)) {
            throw new IllegalArgumentException("A verdict is invalid if, and only if, it has an error finding: " + outcome + " " + findings);
        }
        if (outcome == Outcome.INDETERMINATE && details.isPresent()) {
            throw new IllegalArgumentException("An indeterminate verdict has no details");
        }
    }

    public static <D> Verdict<D> valid(D details, Finding... findings) {
        return new Verdict<>(Outcome.VALID, Optional.of(details), List.of(findings));
    }

    public static <D> Verdict<D> invalid(D details, Finding... findings) {
        return new Verdict<>(Outcome.INVALID, Optional.of(details), List.of(findings));
    }

    public static <D> Verdict<D> indeterminate(Finding... findings) {
        return new Verdict<>(Outcome.INDETERMINATE, Optional.empty(), List.of(findings));
    }
}
