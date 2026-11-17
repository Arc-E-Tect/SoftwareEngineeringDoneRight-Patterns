package com.arc_e_tect.validation.async.application.port.inbound.command;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Run a validation as a job: which validation, under which idempotency key, for which request, and
 * the work itself. The request is compared by equality with that of a job submitted under the same
 * key, so it must have value equality, as a use case's command does.
 */
public record SubmitValidationJobCommand(String validation, String idempotencyKey, Object request, Supplier<JobOutcome> work) {

    public SubmitValidationJobCommand {
        Objects.requireNonNull(validation, "validation");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(work, "work");
    }
}
