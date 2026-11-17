package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The details of a name-iban-match")
class NameIbanMatchDetailsTest {

    @Test
    @DisplayName("of an exact match say so")
    void exactSaysSo() {
        assertEquals(Match.EXACT, NameIbanMatchDetails.exact().match());
    }

    @Test
    @DisplayName("of an exact match carry no registered name")
    void exactCarriesNoRegisteredName() {
        assertEquals(Optional.empty(), NameIbanMatchDetails.exact().registeredName());
    }

    @Test
    @DisplayName("of a close match say so")
    void closeSaysSo() {
        assertEquals(Match.CLOSE, NameIbanMatchDetails.close("Jan de Vries").match());
    }

    @Test
    @DisplayName("of a close match carry the registered name, so the payer can correct a typing error")
    void closeCarriesTheRegisteredName() {
        assertEquals(Optional.of("Jan de Vries"), NameIbanMatchDetails.close("Jan de Vries").registeredName());
    }

    @Test
    @DisplayName("of no match say so")
    void noneSaysSo() {
        assertEquals(Match.NONE, NameIbanMatchDetails.none().match());
    }

    @Test
    @DisplayName("of no match carry no registered name, which would disclose somebody else's name")
    void noneCarriesNoRegisteredName() {
        assertEquals(Optional.empty(), NameIbanMatchDetails.none().registeredName());
    }

    @Test
    @DisplayName("of a close match are refused without the registered name")
    void closeIsRefusedWithoutTheRegisteredName() {
        assertThrows(IllegalArgumentException.class, () -> new NameIbanMatchDetails(Match.CLOSE, Optional.empty()));
    }

    @Test
    @DisplayName("of an exact match are refused with a registered name")
    void exactIsRefusedWithARegisteredName() {
        assertThrows(IllegalArgumentException.class, () -> new NameIbanMatchDetails(Match.EXACT, Optional.of("Jan de Vries")));
    }

    @Test
    @DisplayName("are refused without a match")
    void areRefusedWithoutAMatch() {
        assertThrows(NullPointerException.class, () -> new NameIbanMatchDetails(null, Optional.empty()));
    }

    @Test
    @DisplayName("are refused without a registered-name object")
    void areRefusedWithoutARegisteredNameObject() {
        assertThrows(NullPointerException.class, () -> new NameIbanMatchDetails(Match.NONE, null));
    }
}
