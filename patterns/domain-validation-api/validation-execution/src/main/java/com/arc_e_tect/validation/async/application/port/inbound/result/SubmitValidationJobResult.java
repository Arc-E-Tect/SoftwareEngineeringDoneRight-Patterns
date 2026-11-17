package com.arc_e_tect.validation.async.application.port.inbound.result;

/**
 * Every way a submission can end: the job is accepted, newly or as submitted before with this key
 * and an equal request, or the key was used before with a different request.
 */
public sealed interface SubmitValidationJobResult
        permits SubmitValidationJobResult.Accepted, SubmitValidationJobResult.IdempotencyKeyReused {

    record Accepted(ValidationJobView job) implements SubmitValidationJobResult {
    }

    record IdempotencyKeyReused(String idempotencyKey) implements SubmitValidationJobResult {
    }
}
