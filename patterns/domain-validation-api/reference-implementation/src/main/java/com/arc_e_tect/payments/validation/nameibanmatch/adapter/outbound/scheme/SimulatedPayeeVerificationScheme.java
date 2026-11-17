package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A deterministic stand-in for the payee verification scheme: a table of accounts by IBAN.
 * It takes an account's response time before it answers, really, so that a slow account shows
 * the client's timeout at work, and it fails for an account that is unavailable.
 */
public class SimulatedPayeeVerificationScheme implements PayeeVerificationScheme {

    private final Map<String, SimulatedAccount> accounts;

    public SimulatedPayeeVerificationScheme(Map<String, SimulatedAccount> accounts) {
        this.accounts = Map.copyOf(Objects.requireNonNull(accounts, "accounts"));
    }

    @Override
    public Optional<String> registeredNameOf(String iban) {
        SimulatedAccount account = accounts.get(iban);
        if (account == null) {
            return Optional.empty();
        }
        try {
            Thread.sleep(account.responseTime());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new PayeeVerificationSchemeUnavailableException(iban);
        }
        if (!account.available()) {
            throw new PayeeVerificationSchemeUnavailableException(iban);
        }
        return Optional.of(account.registeredName());
    }
}
