package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import java.util.Objects;

/**
 * What the register of account holders says about an IBAN: the name it is registered to,
 * that it does not know the account, or nothing, because it could not be reached in time.
 */
public sealed interface RegisteredAccountHolder
        permits RegisteredAccountHolder.Registered, RegisteredAccountHolder.Unknown, RegisteredAccountHolder.Unavailable {

    /** The account is held by somebody of this name. */
    record Registered(String name) implements RegisteredAccountHolder {

        public Registered {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("A registered account holder has a name");
            }
        }
    }

    /** No account with this IBAN is known. */
    record Unknown() implements RegisteredAccountHolder {
    }

    /** The register could not answer, so whether the name matches is not known. */
    record Unavailable() implements RegisteredAccountHolder {
    }
}
