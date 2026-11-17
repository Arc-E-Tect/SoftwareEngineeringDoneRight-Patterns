package com.arc_e_tect.payments.validation.verdict;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A rule code")
class RuleCodeTest {

    @Test
    @DisplayName("keeps its value")
    void keepsItsValue() {
        assertEquals("NAME_IBAN_MISMATCH", new RuleCode("NAME_IBAN_MISMATCH").value());
    }

    @Test
    @DisplayName("reads as its value")
    void readsAsItsValue() {
        assertEquals("PAYEE_VERIFICATION_UNAVAILABLE", new RuleCode("PAYEE_VERIFICATION_UNAVAILABLE").toString());
    }

    @Test
    @DisplayName("is refused without a value")
    void isRefusedWithoutAValue() {
        assertThrows(NullPointerException.class, () -> new RuleCode(null));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"", "name_iban_mismatch", "NAME-IBAN", "_NAME", "NAME_", "NAME__IBAN", "1NAME"})
    @DisplayName("is refused when it is not upper-case words joined by single underscores")
    void isRefusedWhenNotUpperSnakeCase(String value) {
        assertThrows(IllegalArgumentException.class, () -> new RuleCode(value));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"A", "NAME", "NAME_IBAN_MISMATCH", "RULE_2"})
    @DisplayName("is accepted when it is upper-case words joined by single underscores")
    void isAcceptedWhenUpperSnakeCase(String value) {
        assertEquals(value, new RuleCode(value).value());
    }
}
