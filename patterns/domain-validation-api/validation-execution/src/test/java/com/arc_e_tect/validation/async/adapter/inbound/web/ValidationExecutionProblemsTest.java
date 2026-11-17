package com.arc_e_tect.validation.async.adapter.inbound.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The problems of the asynchronous execution")
class ValidationExecutionProblemsTest {

    private final ValidationExecutionProblems problems = new ValidationExecutionProblems(URI.create("https://api.example.com/problems/"));

    private static ProblemDetail problem(int status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        return problem;
    }

    @Nested
    @DisplayName("for an asynchronous request without an Idempotency-Key")
    class MissingIdempotencyKey {

        private final ErrorResponseException exception = problems.missingIdempotencyKey();

        @Test
        @DisplayName("is the invalid-request problem, naming the header")
        void isTheInvalidRequestProblem() {
            ProblemDetail expected = problem(400, "https://api.example.com/problems/invalid-request", "Invalid Request",
                    "The request does not match the contract.");
            expected.setProperty("errors", List.of(Map.of("message", "is required with Prefer: respond-async", "context", "Idempotency-Key")));

            assertEquals(expected, exception.getBody());
        }

        @Test
        @DisplayName("is not stored")
        void isNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), exception.getHeaders().getCacheControl());
        }
    }

    @Nested
    @DisplayName("for an Idempotency-Key used before with another request")
    class IdempotencyKeyReused {

        private final ErrorResponseException exception = problems.idempotencyKeyReused("3f1c9b0e-6a2d-4f7e-9c41-0b8a5e2d7c11");

        @Test
        @DisplayName("is the idempotency-key-reused problem, naming the key")
        void isTheIdempotencyKeyReusedProblem() {
            assertEquals(problem(422, "https://api.example.com/problems/idempotency-key-reused", "Idempotency Key Reused",
                            "The Idempotency-Key 3f1c9b0e-6a2d-4f7e-9c41-0b8a5e2d7c11 was used before, with a different request body."),
                    exception.getBody());
        }

        @Test
        @DisplayName("is not stored")
        void isNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), exception.getHeaders().getCacheControl());
        }
    }

    @Nested
    @DisplayName("for a validation job that does not exist")
    class JobNotFound {

        private final ErrorResponseException exception = problems.jobNotFound("name-iban-match", "8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f");

        @Test
        @DisplayName("is the validation-job-not-found problem, naming the validation and the identifier")
        void isTheJobNotFoundProblem() {
            assertEquals(problem(404, "https://api.example.com/problems/validation-job-not-found", "Validation Job Not Found",
                            "No name-iban-match validation job has the identifier 8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f."),
                    exception.getBody());
        }

        @Test
        @DisplayName("is not stored")
        void isNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), exception.getHeaders().getCacheControl());
        }
    }
}
