package com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The command to validate a name against an IBAN")
class ValidateNameIbanMatchCommandTest {

    private static final String IBAN = "NL91ABNA0417164300";
    private static final String NAME = "Jan de Vries";

    @Test
    @DisplayName("keeps the IBAN")
    void keepsTheIban() {
        assertEquals(IBAN, new ValidateNameIbanMatchCommand(IBAN, NAME).iban());
    }

    @Test
    @DisplayName("keeps the name of the account holder")
    void keepsTheName() {
        assertEquals(NAME, new ValidateNameIbanMatchCommand(IBAN, NAME).accountHolderName());
    }

    @Test
    @DisplayName("is equal to a command with the same IBAN and name")
    void isEqualByValue() {
        assertEquals(new ValidateNameIbanMatchCommand(IBAN, NAME), new ValidateNameIbanMatchCommand(IBAN, NAME));
    }

    @Test
    @DisplayName("accepts an IBAN whose checksum fails, which the domain refuses, not the command")
    void acceptsAnIbanWithAFailingChecksum() {
        assertEquals("NL91ABNA0417164301", new ValidateNameIbanMatchCommand("NL91ABNA0417164301", NAME).iban());
    }

    @Test
    @DisplayName("accepts a name of 70 characters")
    void acceptsANameOfSeventyCharacters() {
        String name = "a".repeat(70);

        assertEquals(name, new ValidateNameIbanMatchCommand(IBAN, name).accountHolderName());
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"", "nl91abna0417164300", "NL91 ABNA 0417 1643 00", "NL91ABNA041716", "NL91ABNA04171643001234567890ABCDEFG"})
    @DisplayName("is refused with something that is not an IBAN in electronic format")
    void isRefusedWithAMalformedIban(String iban) {
        assertThrows(IllegalArgumentException.class, () -> new ValidateNameIbanMatchCommand(iban, NAME));
    }

    @Test
    @DisplayName("is refused without an IBAN")
    void isRefusedWithoutAnIban() {
        assertThrows(NullPointerException.class, () -> new ValidateNameIbanMatchCommand(null, NAME));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"", "  "})
    @DisplayName("is refused with a blank name")
    void isRefusedWithABlankName(String name) {
        assertThrows(IllegalArgumentException.class, () -> new ValidateNameIbanMatchCommand(IBAN, name));
    }

    @Test
    @DisplayName("is refused with a name of more than 70 characters")
    void isRefusedWithALongerName() {
        String name = "a".repeat(71);

        assertThrows(IllegalArgumentException.class, () -> new ValidateNameIbanMatchCommand(IBAN, name));
    }

    @Test
    @DisplayName("is refused without a name")
    void isRefusedWithoutAName() {
        assertThrows(NullPointerException.class, () -> new ValidateNameIbanMatchCommand(IBAN, null));
    }
}
