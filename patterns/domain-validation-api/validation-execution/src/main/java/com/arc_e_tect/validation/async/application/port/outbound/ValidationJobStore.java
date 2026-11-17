package com.arc_e_tect.validation.async.application.port.outbound;

import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;

import java.util.Optional;

/** Where validation jobs are kept; a bounded context supplies its own, or uses the in-memory one. */
public interface ValidationJobStore {

    /**
     * Stores the candidate, unless the store holds a job of the same validation with the same
     * idempotency key; atomically, so that two submissions with one key give one job.
     *
     * @return the job the store holds for that validation and key: the existing one, or the candidate
     */
    ValidationJob claim(ValidationJob candidate);

    void update(ValidationJob job);

    Optional<ValidationJob> find(String validation, String validationId);
}
