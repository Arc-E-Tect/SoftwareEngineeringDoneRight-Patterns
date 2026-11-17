package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.Iban;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.RegisteredAccountHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The client of the payee verification scheme")
class PayeeVerificationSchemeClientTest {

    private static final Iban IBAN = new Iban("NL91ABNA0417164300");
    private static final Duration TIMEOUT = Duration.ofMillis(50);
    private static final Duration GENEROUS_TIMEOUT = Duration.ofSeconds(10);

    private final CountDownLatch schemeInterrupted = new CountDownLatch(1);
    /** A scheme that does not answer within a minute, and notes when it is told to stop waiting. */
    private final PayeeVerificationScheme hanging = iban -> {
        try {
            Thread.sleep(Duration.ofMinutes(1));
        } catch (InterruptedException interrupted) {
            schemeInterrupted.countDown();
        }
        return Optional.empty();
    };

    @AfterEach
    void clearTheInterruptFlag() {
        Thread.interrupted();
    }

    @Nested
    @DisplayName("when the scheme answers in time")
    class WhenTheSchemeAnswers {

        @Test
        @DisplayName("asks the scheme about the IBAN")
        void asksAboutTheIban() {
            List<String> asked = new ArrayList<>();
            PayeeVerificationScheme scheme = iban -> {
                asked.add(iban);
                return Optional.empty();
            };

            new PayeeVerificationSchemeClient(scheme, GENEROUS_TIMEOUT).accountHolderOf(IBAN);

            assertEquals(List.of("NL91ABNA0417164300"), asked);
        }

        @Test
        @DisplayName("answers the registered account holder for a name")
        void answersTheRegisteredHolder() {
            PayeeVerificationScheme scheme = iban -> Optional.of("Jan de Vries");

            assertEquals(new RegisteredAccountHolder.Registered("Jan de Vries"),
                    new PayeeVerificationSchemeClient(scheme, GENEROUS_TIMEOUT).accountHolderOf(IBAN));
        }

        @Test
        @DisplayName("answers an unknown account holder for no name")
        void answersAnUnknownHolder() {
            PayeeVerificationScheme scheme = iban -> Optional.empty();

            assertEquals(new RegisteredAccountHolder.Unknown(),
                    new PayeeVerificationSchemeClient(scheme, GENEROUS_TIMEOUT).accountHolderOf(IBAN));
        }
    }

    @Nested
    @DisplayName("when the scheme cannot answer")
    class WhenTheSchemeCannotAnswer {

        @Test
        @DisplayName("answers unavailable when the scheme fails")
        void answersUnavailableWhenTheSchemeFails() {
            PayeeVerificationScheme scheme = iban -> {
                throw new PayeeVerificationSchemeUnavailableException("NL91ABNA0417164300");
            };

            assertEquals(new RegisteredAccountHolder.Unavailable(),
                    new PayeeVerificationSchemeClient(scheme, GENEROUS_TIMEOUT).accountHolderOf(IBAN));
        }

        @Test
        @DisplayName("answers unavailable when the scheme does not answer in time")
        void answersUnavailableWhenTooSlow() {
            assertEquals(new RegisteredAccountHolder.Unavailable(),
                    new PayeeVerificationSchemeClient(hanging, TIMEOUT).accountHolderOf(IBAN));
        }

        @Test
        @DisplayName("stops waiting for the scheme when it does not answer in time")
        void stopsWaitingForTheScheme() throws InterruptedException {
            new PayeeVerificationSchemeClient(hanging, TIMEOUT).accountHolderOf(IBAN);

            assertTrue(schemeInterrupted.await(10, TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("answers unavailable when it is interrupted while it waits")
        void answersUnavailableWhenInterrupted() {
            Thread.currentThread().interrupt();

            assertEquals(new RegisteredAccountHolder.Unavailable(),
                    new PayeeVerificationSchemeClient(hanging, GENEROUS_TIMEOUT).accountHolderOf(IBAN));
        }

        @Test
        @DisplayName("keeps the thread interrupted when it is interrupted while it waits")
        void keepsTheThreadInterrupted() {
            Thread.currentThread().interrupt();

            new PayeeVerificationSchemeClient(hanging, GENEROUS_TIMEOUT).accountHolderOf(IBAN);

            assertTrue(Thread.currentThread().isInterrupted());
        }
    }

    @Nested
    @DisplayName("is refused")
    class IsRefused {

        @ParameterizedTest(name = "{0} ms")
        @ValueSource(longs = {0, -1})
        @DisplayName("with a timeout that is not positive")
        void withATimeoutThatIsNotPositive(long millis) {
            Duration timeout = Duration.ofMillis(millis);

            assertThrows(IllegalArgumentException.class, () -> new PayeeVerificationSchemeClient(hanging, timeout));
        }

        @Test
        @DisplayName("without a timeout")
        void withoutATimeout() {
            assertThrows(NullPointerException.class, () -> new PayeeVerificationSchemeClient(hanging, null));
        }

        @Test
        @DisplayName("without a scheme")
        void withoutAScheme() {
            assertThrows(NullPointerException.class, () -> new PayeeVerificationSchemeClient(null, TIMEOUT));
        }
    }
}
