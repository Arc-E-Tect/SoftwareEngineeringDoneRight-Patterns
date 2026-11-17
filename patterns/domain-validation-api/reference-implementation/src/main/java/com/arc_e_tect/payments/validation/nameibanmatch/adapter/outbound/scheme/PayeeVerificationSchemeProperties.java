package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * The settings of the payee verification scheme: how long the client waits for it and, because it
 * is simulated, the accounts it knows.
 */
@ConfigurationProperties("payments.validation.name-iban-match.scheme")
public record PayeeVerificationSchemeProperties(Duration timeout, @DefaultValue List<Account> accounts) {

    public PayeeVerificationSchemeProperties {
        Objects.requireNonNull(timeout, "timeout");
        accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts"));
    }

    /** An account the simulated scheme knows. */
    public record Account(String iban, String registeredName, @DefaultValue("0s") Duration responseTime,
                          @DefaultValue("true") boolean available) {

        public SimulatedAccount simulated() {
            return new SimulatedAccount(registeredName, responseTime, available);
        }
    }
}
