package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

import java.time.Duration;
import java.util.Objects;

/**
 * An account in the simulated payee verification scheme: the name it is registered to, how long
 * the scheme takes to answer for it, and whether it can answer at all.
 */
public record SimulatedAccount(String registeredName, Duration responseTime, boolean available) {

    public SimulatedAccount {
        Objects.requireNonNull(registeredName, "registeredName");
        Objects.requireNonNull(responseTime, "responseTime");
        if (registeredName.isBlank()) {
            throw new IllegalArgumentException("A simulated account is registered to a name");
        }
        if (responseTime.isNegative()) {
            throw new IllegalArgumentException("A response time is not negative: " + responseTime);
        }
    }

    /** An account the scheme answers for at once. */
    public static SimulatedAccount of(String registeredName) {
        return new SimulatedAccount(registeredName, Duration.ZERO, true);
    }

    /** The same account, which the scheme takes this long to answer for. */
    public SimulatedAccount slow(Duration responseTime) {
        return new SimulatedAccount(registeredName, responseTime, available);
    }

    /** The same account, which the scheme cannot answer for. */
    public SimulatedAccount unavailable() {
        return new SimulatedAccount(registeredName, responseTime, false);
    }
}
