package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A payee claim")
class PayeeClaimTest {

    private static final Iban IBAN = new Iban("NL91ABNA0417164300");

    @Test
    @DisplayName("keeps the IBAN it is about")
    void keepsItsIban() {
        assertEquals(IBAN, new PayeeClaim(IBAN, "Jan de Vries").iban());
    }

    @Test
    @DisplayName("keeps the name the payer gives")
    void keepsTheName() {
        assertEquals("Jan de Vries", new PayeeClaim(IBAN, "Jan de Vries").accountHolderName());
    }

    @Test
    @DisplayName("accepts a name of 70 characters, the SEPA limit")
    void acceptsANameOfSeventyCharacters() {
        String name = "a".repeat(70);

        assertEquals(name, new PayeeClaim(IBAN, name).accountHolderName());
    }

    @Test
    @DisplayName("is refused with a name of more than 70 characters")
    void isRefusedWithALongerName() {
        String name = "a".repeat(71);

        assertThrows(IllegalArgumentException.class, () -> new PayeeClaim(IBAN, name));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"", "  ", "\t\n", "  ", " ", " ", " "})
    @DisplayName("is refused with a name of only whitespace, Unicode's: an em space, a no-break space")
    void isRefusedWithABlankName(String name) {
        assertThrows(IllegalArgumentException.class, () -> new PayeeClaim(IBAN, name));
    }

    @Test
    @DisplayName("accepts a name with a no-break space in it")
    void acceptsANameWithANoBreakSpace() {
        assertEquals("Jan de Vries", new PayeeClaim(IBAN, "Jan de Vries").accountHolderName());
    }

    @Test
    @DisplayName("is refused without a name")
    void isRefusedWithoutAName() {
        assertThrows(NullPointerException.class, () -> new PayeeClaim(IBAN, null));
    }

    @Test
    @DisplayName("is refused without an IBAN")
    void isRefusedWithoutAnIban() {
        assertThrows(NullPointerException.class, () -> new PayeeClaim(null, "Jan de Vries"));
    }
}
