package com.arc_e_tect.validation.async.application.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A validation job has run: it completed, with the validation's outcome, or it failed. It refers to
 * the job and carries neither its request nor its result; whoever is told reads those from the job
 * resource at its location.
 */
public record ValidationCompleted(String validationId, String validation, ValidationJob.Status status, Optional<Outcome> outcome,
                                  Instant completedAt, String location) {

    public ValidationCompleted {
        Objects.requireNonNull(validationId, "validationId");
        Objects.requireNonNull(validation, "validation");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(completedAt, "completedAt");
        Objects.requireNonNull(location, "location");
        if (status == ValidationJob.Status.PENDING) {
            throw new IllegalArgumentException("A job completes once it has run; this one is pending");
        }
        if (outcome.isPresent() != (status == ValidationJob.Status.COMPLETED)) {
            throw new IllegalArgumentException("A job has an outcome if, and only if, it completed: " + status);
        }
    }

    /** The completion of a job that has run, at that moment. */
    public static ValidationCompleted of(ValidationJob job, Optional<Outcome> outcome, Instant completedAt) {
        return new ValidationCompleted(job.validationId(), job.validation(), job.status(), outcome, completedAt, job.location());
    }
}
