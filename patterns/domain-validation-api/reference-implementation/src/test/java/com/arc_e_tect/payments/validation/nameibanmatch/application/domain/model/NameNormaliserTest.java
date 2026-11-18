package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Normalising a name")
class NameNormaliserTest {

    @ParameterizedTest(name = "\"{0}\" becomes \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "Jan de Vries|jan de vries",
            "JAN DE VRIES|jan de vries",
            "'  Jan   de\tVries  '|jan de vries",
            "Zoë Müller-Lüdenscheidt|zoe muller-ludenscheidt",
            "François Ångström|francois angstrom",
            "jan de vries|jan de vries"
    })
    @DisplayName("ignores case, surrounding and repeated whitespace, and diacritics")
    void ignoresCaseWhitespaceAndDiacritics(String name, String normalised) {
        assertEquals(normalised, new NameNormaliser().normalise(name));
    }

    @ParameterizedTest(name = "\"{0}\" becomes \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "'Jan de Vries'|jan de vries",
            "'Jan  de Vries'|jan de vries",
            "' Jan de Vries '|jan de vries"
    })
    @DisplayName("treats Unicode whitespace as whitespace: an em space, a no-break space")
    void treatsUnicodeWhitespaceAsWhitespace(String name, String normalised) {
        assertEquals(normalised, new NameNormaliser().normalise(name));
    }

    @ParameterizedTest(name = "\"{0}\" stays \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "Strauß|strauß",
            "Søren|søren"
    })
    @DisplayName("transliterates no letter")
    void transliteratesNoLetter(String name, String normalised) {
        assertEquals(normalised, new NameNormaliser().normalise(name));
    }
}
