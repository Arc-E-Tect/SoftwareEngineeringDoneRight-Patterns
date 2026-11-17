package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The supported countries")
class SupportedCountriesTest {

    private final SupportedCountries supported = SupportedCountries.of("NL", "DE");

    @Test
    @DisplayName("cover an IBAN of one of their countries")
    void coverAnIbanOfTheirCountry() {
        assertTrue(supported.covers(new Iban("DE89370400440532013000")));
    }

    @Test
    @DisplayName("do not cover an IBAN of another country")
    void doNotCoverAnIbanOfAnotherCountry() {
        assertFalse(supported.covers(new Iban("BE68539007547034")));
    }

    @Test
    @DisplayName("keep the country codes they are given")
    void keepTheirCountryCodes() {
        assertEquals(Set.of("NL", "DE"), supported.countryCodes());
    }

    @Test
    @DisplayName("do not change when the set they were made from changes")
    void doNotChangeWithTheirSource() {
        Set<String> codes = new HashSet<>(Set.of("NL"));
        SupportedCountries fromCodes = new SupportedCountries(codes);

        codes.add("DE");

        assertEquals(Set.of("NL"), fromCodes.countryCodes());
    }

    @Test
    @DisplayName("are refused without any country")
    void areRefusedWithoutAnyCountry() {
        assertThrows(IllegalArgumentException.class, SupportedCountries::of);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"nl", "N", "NLD", "N1", ""})
    @DisplayName("are refused with a code that is not two upper-case letters")
    void areRefusedWithAMalformedCode(String code) {
        assertThrows(IllegalArgumentException.class, () -> SupportedCountries.of("NL", code));
    }

    @Test
    @DisplayName("are refused without a set of codes")
    void areRefusedWithoutASet() {
        assertThrows(NullPointerException.class, () -> new SupportedCountries(null));
    }

    @Test
    @DisplayName("are refused with a missing code")
    void areRefusedWithAMissingCode() {
        assertThrows(NullPointerException.class, () -> SupportedCountries.of("NL", null));
    }
}
