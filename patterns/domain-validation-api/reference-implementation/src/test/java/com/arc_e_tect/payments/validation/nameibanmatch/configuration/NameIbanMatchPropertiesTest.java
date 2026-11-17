package com.arc_e_tect.payments.validation.nameibanmatch.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The settings of the name-iban-match validation")
class NameIbanMatchPropertiesTest {

    @Test
    @DisplayName("do not change when the countries they were made from change")
    void doNotChangeWithTheirCountries() {
        Set<String> countries = new HashSet<>(Set.of("NL"));
        NameIbanMatchProperties properties = new NameIbanMatchProperties(countries);

        countries.add("DE");

        assertEquals(Set.of("NL"), properties.supportedCountries());
    }

    @Test
    @DisplayName("are refused without supported countries")
    void areRefusedWithoutSupportedCountries() {
        assertThrows(NullPointerException.class, () -> new NameIbanMatchProperties(null));
    }
}
