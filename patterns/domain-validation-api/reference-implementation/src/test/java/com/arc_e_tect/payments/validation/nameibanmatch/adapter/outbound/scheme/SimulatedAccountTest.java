package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A simulated account")
class SimulatedAccountTest {

    @Test
    @DisplayName("answers at once and is available, unless told otherwise")
    void answersAtOnceAndIsAvailable() {
        assertEquals(new SimulatedAccount("Jan de Vries", Duration.ZERO, true), SimulatedAccount.of("Jan de Vries"));
    }

    @Test
    @DisplayName("can be slow to answer")
    void canBeSlow() {
        assertEquals(new SimulatedAccount("Jan de Vries", Duration.ofSeconds(5), true),
                SimulatedAccount.of("Jan de Vries").slow(Duration.ofSeconds(5)));
    }

    @Test
    @DisplayName("can be unavailable")
    void canBeUnavailable() {
        assertEquals(new SimulatedAccount("Jan de Vries", Duration.ZERO, false),
                SimulatedAccount.of("Jan de Vries").unavailable());
    }

    @Test
    @DisplayName("is refused with a blank name")
    void isRefusedWithABlankName() {
        assertThrows(IllegalArgumentException.class, () -> SimulatedAccount.of("  "));
    }

    @Test
    @DisplayName("is refused without a name")
    void isRefusedWithoutAName() {
        assertThrows(NullPointerException.class, () -> SimulatedAccount.of(null));
    }

    @Test
    @DisplayName("is refused with a negative response time")
    void isRefusedWithANegativeResponseTime() {
        assertThrows(IllegalArgumentException.class, () -> new SimulatedAccount("Jan de Vries", Duration.ofMillis(-1), true));
    }

    @Test
    @DisplayName("is refused without a response time")
    void isRefusedWithoutAResponseTime() {
        assertThrows(NullPointerException.class, () -> new SimulatedAccount("Jan de Vries", null, true));
    }
}
