package com.arc_e_tect.validation.async.application.domain.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A validation run asynchronously: pending until it has run, then completed with the validation's
 * result, or failed with a problem. It keeps the idempotency key it was submitted with, and the
 * request, so that a submission with the same key can be told apart from a different one, and its
 * location: the path of its job resource.
 */
public record ValidationJob(String validationId, String validation, String idempotencyKey, Object request, String location,
                            Status status, Optional<Object> result, Optional<JobProblem> problem) {

    public enum Status { PENDING, COMPLETED, FAILED }

    public ValidationJob {
        Objects.requireNonNull(validationId, "validationId");
        Objects.requireNonNull(validation, "validation");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(problem, "problem");
        if (result.isPresent() != (status == Status.COMPLETED)) {
            throw new IllegalArgumentException("A job has a result if, and only if, it completed: " + status);
        }
        if (problem.isPresent() != (status == Status.FAILED)) {
            throw new IllegalArgumentException("A job has a problem if, and only if, it failed: " + status);
        }
    }

    /**
     * A new job, with an identifier of its own, which has not run yet.
     *
     * @param operationPath the path of the validation's operation; the job resource is that path followed by the job's identifier
     */
    public static ValidationJob pending(String validation, String idempotencyKey, Object request, String operationPath) {
        Objects.requireNonNull(operationPath, "operationPath");
        String validationId = UUID.randomUUID().toString();
        return new ValidationJob(validationId, validation, idempotencyKey, request, operationPath + "/" + validationId,
                Status.PENDING, Optional.empty(), Optional.empty());
    }

    public ValidationJob completed(Object result) {
        return ran(Status.COMPLETED, Optional.of(result), Optional.empty());
    }

    public ValidationJob failed(JobProblem problem) {
        return ran(Status.FAILED, Optional.empty(), Optional.of(problem));
    }

    private ValidationJob ran(Status ended, Optional<Object> result, Optional<JobProblem> problem) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("A job runs once; this one is " + status);
        }
        return new ValidationJob(validationId, validation, idempotencyKey, request, location, ended, result, problem);
    }
}
