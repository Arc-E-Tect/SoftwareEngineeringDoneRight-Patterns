package com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Every way {@code ValidateNameIbanMatchUseCase} can end, so that its inbound adapter handles each one:
 * the rule was checked, whatever the outcome, or the domain refused the request.
 *
 * <p>The port owns these types, so that no inbound adapter depends on the domain model.
 */
public sealed interface ValidateNameIbanMatchResult
        permits ValidateNameIbanMatchResult.Checked,
                ValidateNameIbanMatchResult.UnsupportedCountry,
                ValidateNameIbanMatchResult.InvalidChecksum {

    enum Outcome { VALID, INVALID, INDETERMINATE }

    enum Match { EXACT, CLOSE, NONE }

    enum Severity { ERROR, WARNING, INFO }

    /** One thing the rule found, with a JSON Pointer to the part of the input it is about, if any. */
    record Finding(String rule, Severity severity, String message, Optional<String> pointer) {

        public Finding {
            Objects.requireNonNull(rule, "rule");
            Objects.requireNonNull(severity, "severity");
            Objects.requireNonNull(message, "message");
            Objects.requireNonNull(pointer, "pointer");
        }
    }

    /** How the name matches; the registered name only for a close match. */
    record Details(Match match, Optional<String> registeredName) {

        public Details {
            Objects.requireNonNull(match, "match");
            Objects.requireNonNull(registeredName, "registeredName");
        }
    }

    /** The rule was checked: a verdict, whatever its outcome. There are no details when no verdict was reached. */
    record Checked(Outcome outcome, Optional<Details> details, List<Finding> findings)
            implements ValidateNameIbanMatchResult {

        public Checked {
            Objects.requireNonNull(outcome, "outcome");
            Objects.requireNonNull(details, "details");
            findings = List.copyOf(findings);
        }
    }

    /** The IBAN is well-formed, but the payee verification scheme does not cover its country. */
    record UnsupportedCountry(String countryCode) implements ValidateNameIbanMatchResult {

        public UnsupportedCountry {
            Objects.requireNonNull(countryCode, "countryCode");
        }
    }

    /** The IBAN is well-formed, but its check digits are wrong. */
    record InvalidChecksum(String iban) implements ValidateNameIbanMatchResult {

        public InvalidChecksum {
            Objects.requireNonNull(iban, "iban");
        }
    }
}
