package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** What the payer claims: that the account with this IBAN is held by somebody of this name. */
public record PayeeClaim(Iban iban, String accountHolderName) {

    /** The SEPA limit for the name of a party. */
    static final int MAXIMUM_NAME_LENGTH = 70;

    /** Only whitespace, Unicode's, as names are normalised: unlike {@link String#isBlank()}, a no-break space too. */
    private static final Pattern BLANK = Pattern.compile("\\s*", Pattern.UNICODE_CHARACTER_CLASS);

    public PayeeClaim {
        Objects.requireNonNull(iban, "iban");
        Objects.requireNonNull(accountHolderName, "accountHolderName");
        if (BLANK.matcher(accountHolderName).matches()) {
            throw new IllegalArgumentException("A payee claim needs the name of the account holder");
        }
        if (accountHolderName.length() > MAXIMUM_NAME_LENGTH) {
            throw new IllegalArgumentException("The name of an account holder has at most " + MAXIMUM_NAME_LENGTH + " characters");
        }
    }
}
