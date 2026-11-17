package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A registered account holder")
class RegisteredAccountHolderTest {

    @Test
    @DisplayName("keeps the registered name")
    void keepsTheName() {
        assertEquals("Jan de Vries", new RegisteredAccountHolder.Registered("Jan de Vries").name());
    }

    @Test
    @DisplayName("is refused without a name")
    void isRefusedWithoutAName() {
        assertThrows(NullPointerException.class, () -> new RegisteredAccountHolder.Registered(null));
    }

    @Test
    @DisplayName("is refused with a blank name")
    void isRefusedWithABlankName() {
        assertThrows(IllegalArgumentException.class, () -> new RegisteredAccountHolder.Registered(" "));
    }
}
