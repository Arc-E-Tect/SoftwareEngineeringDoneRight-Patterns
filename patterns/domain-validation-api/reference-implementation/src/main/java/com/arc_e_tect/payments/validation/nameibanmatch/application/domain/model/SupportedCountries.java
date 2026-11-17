package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The countries whose accounts the payee verification scheme covers, by ISO 3166-1 alpha-2 code.
 * An IBAN of any other country cannot be checked, so the domain refuses it.
 */
public record SupportedCountries(Set<String> countryCodes) {

    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

    public SupportedCountries {
        countryCodes = Set.copyOf(Objects.requireNonNull(countryCodes, "countryCodes"));
        if (countryCodes.isEmpty()) {
            throw new IllegalArgumentException("A scheme supports at least one country");
        }
        countryCodes.stream().filter(code -> !COUNTRY_CODE.matcher(code).matches()).findAny().ifPresent(code -> {
            throw new IllegalArgumentException("A country code is two upper-case letters: " + code);
        });
    }

    public static SupportedCountries of(String... countryCodes) {
        return new SupportedCountries(Set.of(countryCodes));
    }

    /** Whether the scheme covers the account with this IBAN. */
    public boolean covers(Iban iban) {
        return countryCodes.contains(iban.countryCode());
    }
}
