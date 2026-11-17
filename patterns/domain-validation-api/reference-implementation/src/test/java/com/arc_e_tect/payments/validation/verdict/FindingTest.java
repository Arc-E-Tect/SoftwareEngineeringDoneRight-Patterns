package com.arc_e_tect.payments.validation.verdict;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A finding")
class FindingTest {

    private static final RuleCode RULE = new RuleCode("NAME_IBAN_MISMATCH");

    @Test
    @DisplayName("refers to no part of the input unless it is placed")
    void hasNoPointerUnlessPlaced() {
        assertEquals(Optional.empty(), Finding.of(RULE, Severity.ERROR, "Wrong.").pointer());
    }

    @Test
    @DisplayName("refers to the part of the input it is placed at")
    void refersToWhereItIsPlaced() {
        assertEquals(Optional.of("/accountHolderName"), Finding.of(RULE, Severity.ERROR, "Wrong.").at("/accountHolderName").pointer());
    }

    @Test
    @DisplayName("keeps its rule when it is placed")
    void keepsItsRuleWhenPlaced() {
        assertEquals(RULE, Finding.of(RULE, Severity.ERROR, "Wrong.").at("/iban").rule());
    }

    @Test
    @DisplayName("keeps its severity when it is placed")
    void keepsItsSeverityWhenPlaced() {
        assertEquals(Severity.WARNING, Finding.of(RULE, Severity.WARNING, "Careful.").at("/iban").severity());
    }

    @Test
    @DisplayName("keeps its message when it is placed")
    void keepsItsMessageWhenPlaced() {
        assertEquals("Careful.", Finding.of(RULE, Severity.WARNING, "Careful.").at("/iban").message());
    }

    @Test
    @DisplayName("is refused without a rule")
    void isRefusedWithoutARule() {
        assertThrows(NullPointerException.class, () -> Finding.of(null, Severity.ERROR, "Wrong."));
    }

    @Test
    @DisplayName("is refused without a severity")
    void isRefusedWithoutASeverity() {
        assertThrows(NullPointerException.class, () -> Finding.of(RULE, null, "Wrong."));
    }

    @Test
    @DisplayName("is refused without a message")
    void isRefusedWithoutAMessage() {
        assertThrows(NullPointerException.class, () -> Finding.of(RULE, Severity.ERROR, null));
    }

    @Test
    @DisplayName("is refused with a blank message")
    void isRefusedWithABlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> Finding.of(RULE, Severity.ERROR, " "));
    }

    @Test
    @DisplayName("is refused without a pointer object")
    void isRefusedWithoutAPointerObject() {
        assertThrows(NullPointerException.class, () -> new Finding(RULE, Severity.ERROR, "Wrong.", null));
    }

    @Test
    @DisplayName("is refused at a pointer that is not a JSON Pointer into the input")
    void isRefusedAtAPointerThatDoesNotStartWithASlash() {
        Finding finding = Finding.of(RULE, Severity.ERROR, "Wrong.");

        assertThrows(IllegalArgumentException.class, () -> finding.at("accountHolderName"));
    }

    @Test
    @DisplayName("is refused at the empty pointer, which refers to the whole input rather than a part of it")
    void isRefusedAtTheEmptyPointer() {
        Finding finding = Finding.of(RULE, Severity.ERROR, "Wrong.");

        assertThrows(IllegalArgumentException.class, () -> finding.at(""));
    }
}
