package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme.PayeeVerificationSchemeProperties.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The settings of the payee verification scheme")
class PayeeVerificationSchemePropertiesTest {

    private static final Account JAN = new Account("NL91ABNA0417164300", "Jan de Vries", Duration.ofSeconds(5), false);

    @Test
    @DisplayName("describe an account as the simulated scheme knows it")
    void describeASimulatedAccount() {
        assertEquals(SimulatedAccount.of("Jan de Vries").slow(Duration.ofSeconds(5)).unavailable(), JAN.simulated());
    }

    @Test
    @DisplayName("do not change when the accounts they were made from change")
    void doNotChangeWithTheirAccounts() {
        List<Account> accounts = new ArrayList<>(List.of(JAN));
        PayeeVerificationSchemeProperties properties = new PayeeVerificationSchemeProperties(Duration.ofSeconds(2), accounts);

        accounts.clear();

        assertEquals(List.of(JAN), properties.accounts());
    }

    @Test
    @DisplayName("are refused without a timeout")
    void areRefusedWithoutATimeout() {
        assertThrows(NullPointerException.class, () -> new PayeeVerificationSchemeProperties(null, List.of(JAN)));
    }

    @Test
    @DisplayName("are refused without a list of accounts")
    void areRefusedWithoutAccounts() {
        assertThrows(NullPointerException.class, () -> new PayeeVerificationSchemeProperties(Duration.ofSeconds(2), null));
    }
}
