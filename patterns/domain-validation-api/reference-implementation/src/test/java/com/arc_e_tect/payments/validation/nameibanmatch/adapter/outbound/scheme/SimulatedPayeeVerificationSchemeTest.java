package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The simulated payee verification scheme")
class SimulatedPayeeVerificationSchemeTest {

    private static final String JAN = "NL91ABNA0417164300";
    private static final String SLOW = "DE89370400440532013000";
    private static final String UNAVAILABLE = "NL02ABNA0123456789";
    private static final Duration RESPONSE_TIME = Duration.ofMillis(50);

    private final SimulatedPayeeVerificationScheme scheme = new SimulatedPayeeVerificationScheme(Map.of(
            JAN, SimulatedAccount.of("Jan de Vries"),
            SLOW, SimulatedAccount.of("Anna Schmidt").slow(RESPONSE_TIME),
            UNAVAILABLE, SimulatedAccount.of("Piet Jansen").unavailable()));

    @AfterEach
    void clearTheInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    @DisplayName("answers the registered name of an account in its table")
    void answersTheRegisteredName() {
        assertEquals(Optional.of("Jan de Vries"), scheme.registeredNameOf(JAN));
    }

    @Test
    @DisplayName("answers no name for an account it does not know")
    void answersNoNameForAnUnknownAccount() {
        assertEquals(Optional.empty(), scheme.registeredNameOf("BE68539007547034"));
    }

    @Test
    @DisplayName("fails as unavailable for an unavailable account")
    void failsForAnUnavailableAccount() {
        assertThrows(PayeeVerificationSchemeUnavailableException.class, () -> scheme.registeredNameOf(UNAVAILABLE));
    }

    @Test
    @DisplayName("takes at least the response time of a slow account")
    void takesTheResponseTimeOfASlowAccount() {
        long start = System.nanoTime();

        scheme.registeredNameOf(SLOW);

        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(RESPONSE_TIME) >= 0);
    }

    @Test
    @DisplayName("fails as unavailable when it is interrupted while it takes its time")
    void failsWhenInterrupted() {
        Thread.currentThread().interrupt();

        assertThrows(PayeeVerificationSchemeUnavailableException.class, () -> scheme.registeredNameOf(SLOW));
    }

    @Test
    @DisplayName("keeps the thread interrupted when it is interrupted while it takes its time")
    void keepsTheThreadInterrupted() {
        Thread.currentThread().interrupt();
        try {
            scheme.registeredNameOf(SLOW);
        } catch (PayeeVerificationSchemeUnavailableException expected) {
            // That it fails is the previous test's concern.
        }

        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    @DisplayName("does not change when the table it was made from changes")
    void doesNotChangeWithItsSource() {
        Map<String, SimulatedAccount> accounts = new HashMap<>();
        SimulatedPayeeVerificationScheme fromAccounts = new SimulatedPayeeVerificationScheme(accounts);

        accounts.put(JAN, SimulatedAccount.of("Jan de Vries"));

        assertEquals(Optional.empty(), fromAccounts.registeredNameOf(JAN));
    }

    @Test
    @DisplayName("knows the accounts of its settings")
    void knowsTheAccountsOfItsSettings() {
        SimulatedPayeeVerificationScheme configured = new SimulatedPayeeVerificationScheme(new PayeeVerificationSchemeProperties(
                Duration.ofSeconds(2), List.of(new PayeeVerificationSchemeProperties.Account(JAN, "Jan de Vries", Duration.ZERO, true))));

        assertEquals(Optional.of("Jan de Vries"), configured.registeredNameOf(JAN));
    }

    @Test
    @DisplayName("is refused with settings that have an account twice")
    void isRefusedWithAnAccountTwice() {
        PayeeVerificationSchemeProperties twice = new PayeeVerificationSchemeProperties(Duration.ofSeconds(2), List.of(
                new PayeeVerificationSchemeProperties.Account(JAN, "Jan de Vries", Duration.ZERO, true),
                new PayeeVerificationSchemeProperties.Account(JAN, "Jan Jansen", Duration.ZERO, true)));

        assertThrows(IllegalStateException.class, () -> new SimulatedPayeeVerificationScheme(twice));
    }

    @Test
    @DisplayName("is refused without a table of accounts")
    void isRefusedWithoutATable() {
        assertThrows(NullPointerException.class, () -> new SimulatedPayeeVerificationScheme((Map<String, SimulatedAccount>) null));
    }
}
