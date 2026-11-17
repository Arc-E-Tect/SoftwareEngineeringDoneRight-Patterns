package com.arc_e_tect.validation.async.adapter.inbound.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The problems of the asynchronous execution, which every bounded context shares, as RFC 9457
 * Problem Details: their types are names below a base that the bounded context configures.
 */
public class ValidationExecutionProblems {

    private final URI problemTypes;

    public ValidationExecutionProblems(URI problemTypes) {
        this.problemTypes = Objects.requireNonNull(problemTypes, "problemTypes");
    }

    /** An asynchronous request without the Idempotency-Key that makes submitting a job safe to retry. */
    public ErrorResponseException missingIdempotencyKey() {
        ProblemDetail problem = problem(400, "invalid-request", "Invalid Request", "The request does not match the contract.");
        problem.setProperty("errors", List.of(Map.of("message", "is required with Prefer: respond-async", "context", "Idempotency-Key")));
        return answer(problem);
    }

    /** The Idempotency-Key was used before, with a different request. */
    public ErrorResponseException idempotencyKeyReused(String idempotencyKey) {
        return answer(problem(422, "idempotency-key-reused", "Idempotency Key Reused",
                "The Idempotency-Key " + idempotencyKey + " was used before, with a different request body."));
    }

    /** No job of the validation has this identifier. */
    public ErrorResponseException jobNotFound(String validation, String validationId) {
        return answer(problem(404, "validation-job-not-found", "Validation Job Not Found",
                "No " + validation + " validation job has the identifier " + validationId + "."));
    }

    private ProblemDetail problem(int status, String name, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setType(problemTypes.resolve(name));
        problem.setTitle(title);
        return problem;
    }

    private static ErrorResponseException answer(ProblemDetail problem) {
        ErrorResponseException exception = new ErrorResponseException(HttpStatusCode.valueOf(problem.getStatus()), problem, null);
        exception.getHeaders().setCacheControl(CacheControl.noStore());
        return exception;
    }
}
