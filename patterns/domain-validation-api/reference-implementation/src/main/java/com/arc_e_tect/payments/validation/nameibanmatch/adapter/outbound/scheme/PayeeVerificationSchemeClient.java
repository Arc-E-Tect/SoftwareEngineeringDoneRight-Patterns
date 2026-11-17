package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.Iban;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.RegisteredAccountHolder;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.outbound.AccountHolderRegistry;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The register of account holders, answered by the payee verification scheme.
 *
 * <p>It asks the scheme on a virtual thread and waits at most the timeout. When the scheme fails or
 * does not answer in time, the register is unavailable: the client says so, and stops waiting,
 * rather than failing, because what that means is for the use case to decide.
 */
public class PayeeVerificationSchemeClient implements AccountHolderRegistry {

    private final PayeeVerificationScheme scheme;
    private final Duration timeout;

    public PayeeVerificationSchemeClient(PayeeVerificationScheme scheme, Duration timeout) {
        this.scheme = Objects.requireNonNull(scheme, "scheme");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (!timeout.isPositive()) {
            throw new IllegalArgumentException("A timeout is positive: " + timeout);
        }
    }

    @Override
    public RegisteredAccountHolder accountHolderOf(Iban iban) {
        FutureTask<Optional<String>> answer = new FutureTask<>(() -> scheme.registeredNameOf(iban.value()));
        Thread.ofVirtual().start(answer);
        try {
            return answer.get(timeout.toNanos(), TimeUnit.NANOSECONDS)
                    .<RegisteredAccountHolder>map(RegisteredAccountHolder.Registered::new)
                    .orElseGet(RegisteredAccountHolder.Unknown::new);
        } catch (ExecutionException | TimeoutException unanswered) {
            return new RegisteredAccountHolder.Unavailable();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return new RegisteredAccountHolder.Unavailable();
        } finally {
            answer.cancel(true);
        }
    }
}
