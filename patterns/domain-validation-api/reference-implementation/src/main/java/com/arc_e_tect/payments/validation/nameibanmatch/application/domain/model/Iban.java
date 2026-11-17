package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An International Bank Account Number in electronic format: upper case, without spaces.
 * It has the shape of an IBAN; whether its check digits are right is a question it answers
 * (ISO 13616, mod-97), not a condition of its existence, because a wrong checksum is a request
 * the domain refuses, not a malformed one.
 */
public record Iban(String value) {

    private static final Pattern SHAPE = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}");
    private static final int MODULUS = 97;

    public Iban {
        Objects.requireNonNull(value, "value");
        if (!SHAPE.matcher(value).matches()) {
            throw new IllegalArgumentException("Not an IBAN in electronic format: " + value);
        }
    }

    /** The ISO 3166-1 alpha-2 code of the account's country. */
    public String countryCode() {
        return value.substring(0, 2);
    }

    /** Whether the check digits are right: the IBAN, rearranged and read as a number, is 1 modulo 97. */
    public boolean hasValidChecksum() {
        String rearranged = value.substring(4) + value.substring(0, 4);
        int remainder = 0;
        for (char character : rearranged.toCharArray()) {
            int digits = Character.getNumericValue(character);
            int scale = digits < 10 ? 10 : 100;
            remainder = (remainder * scale + digits) % MODULUS;
        }
        return remainder == 1;
    }
}
