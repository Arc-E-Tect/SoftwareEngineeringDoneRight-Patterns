package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * The details of a name-iban-match verdict.
 * The registered name is there for a close match only, so the payer can correct an obvious
 * typing error; for any other match it would disclose a name the payer did not know.
 */
public record NameIbanMatchDetails(Match match, Optional<String> registeredName) {

    public NameIbanMatchDetails {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(registeredName, "registeredName");
        if (registeredName.isPresent() != (match == Match.CLOSE)) {
            throw new IllegalArgumentException("Only a close match carries the registered name");
        }
    }

    public static NameIbanMatchDetails exact() {
        return new NameIbanMatchDetails(Match.EXACT, Optional.empty());
    }

    public static NameIbanMatchDetails close(String registeredName) {
        return new NameIbanMatchDetails(Match.CLOSE, Optional.of(registeredName));
    }

    public static NameIbanMatchDetails none() {
        return new NameIbanMatchDetails(Match.NONE, Optional.empty());
    }
}
