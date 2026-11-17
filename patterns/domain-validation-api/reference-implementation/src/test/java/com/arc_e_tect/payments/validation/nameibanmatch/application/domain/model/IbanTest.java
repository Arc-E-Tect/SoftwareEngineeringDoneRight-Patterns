package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("An IBAN")
class IbanTest {

    @Test
    @DisplayName("keeps its value")
    void keepsItsValue() {
        assertEquals("NL91ABNA0417164300", new Iban("NL91ABNA0417164300").value());
    }

    @Test
    @DisplayName("knows the country of its account")
    void knowsItsCountry() {
        assertEquals("BR", new Iban("BR1800360305000010009795493C1").countryCode());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"NL91ABNA0417164300", "DE89370400440532013000", "BR1800360305000010009795493C1", "GB82WEST12345698765432"})
    @DisplayName("has a valid checksum when its check digits are right")
    void hasAValidChecksum(String value) {
        assertTrue(new Iban(value).hasValidChecksum());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"NL92ABNA0417164300", "DE88370400440532013000", "NL91ABNA0417164301"})
    @DisplayName("has an invalid checksum when its check digits are wrong")
    void hasAnInvalidChecksum(String value) {
        assertFalse(new Iban(value).hasValidChecksum());
    }

    @Test
    @DisplayName("is refused without a value")
    void isRefusedWithoutAValue() {
        assertThrows(NullPointerException.class, () -> new Iban(null));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"", "nl91abna0417164300", "NL91 ABNA 0417 1643 00", "NLAAABNA0417164300", "1191ABNA0417164300",
            "NL91ABNA041716", "NL91ABNA0417164300123456789012345", "NL91ABNA041716430!"})
    @DisplayName("is refused when it does not have the shape of an IBAN in electronic format")
    void isRefusedWithoutTheShapeOfAnIban(String value) {
        assertThrows(IllegalArgumentException.class, () -> new Iban(value));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"NL91ABNA041716430", "NL91ABNA04171643001234567890ABCD"})
    @DisplayName("is accepted at the shortest and the longest shape")
    void isAcceptedAtTheBoundsOfTheShape(String value) {
        assertEquals(value, new Iban(value).value());
    }
}
