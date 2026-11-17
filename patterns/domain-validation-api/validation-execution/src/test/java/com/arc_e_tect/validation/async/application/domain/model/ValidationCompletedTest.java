package com.arc_e_tect.validation.async.application.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The completion of a validation job")
class ValidationCompletedTest {

    private static final Instant AT = Instant.parse("2026-11-17T10:35:02Z");
    private static final JobProblem PROBLEM = new JobProblem(URI.create("https://api.example.com/problems/internal-server-error"),
            "Internal Server Error", 500, "An unexpected error occurred while running the validation.");

    private final ValidationJob pending = ValidationJob.pending("name-iban-match", "key", "request", "/validations");

    @Nested
    @DisplayName("of a job")
    class OfAJob {

        @Test
        @DisplayName("that completed refers to the job, with its outcome")
        void thatCompleted() {
            assertEquals(new ValidationCompleted(pending.validationId(), "name-iban-match", ValidationJob.Status.COMPLETED,
                            Optional.of(Outcome.VALID), AT, pending.location()),
                    ValidationCompleted.of(pending.completed("result"), Optional.of(Outcome.VALID), AT));
        }

        @Test
        @DisplayName("that failed refers to the job, without an outcome")
        void thatFailed() {
            assertEquals(new ValidationCompleted(pending.validationId(), "name-iban-match", ValidationJob.Status.FAILED,
                            Optional.empty(), AT, pending.location()),
                    ValidationCompleted.of(pending.failed(PROBLEM), Optional.empty(), AT));
        }
    }

    @Nested
    @DisplayName("is refused")
    class IsRefused {

        @Test
        @DisplayName("for a job that is still pending")
        void forAPendingJob() {
            assertThrows(IllegalArgumentException.class, () -> ValidationCompleted.of(pending, Optional.empty(), AT));
        }

        @Test
        @DisplayName("for a completed job without an outcome")
        void forACompletedJobWithoutAnOutcome() {
            ValidationJob completed = pending.completed("result");

            assertThrows(IllegalArgumentException.class, () -> ValidationCompleted.of(completed, Optional.empty(), AT));
        }

        @Test
        @DisplayName("for a failed job with an outcome")
        void forAFailedJobWithAnOutcome() {
            ValidationJob failed = pending.failed(PROBLEM);
            Optional<Outcome> outcome = Optional.of(Outcome.INVALID);

            assertThrows(IllegalArgumentException.class, () -> ValidationCompleted.of(failed, outcome, AT));
        }

        @Test
        @DisplayName("without an identifier")
        void withoutAnIdentifier() {
            assertThrows(NullPointerException.class, () -> new ValidationCompleted(null, "name-iban-match",
                    ValidationJob.Status.FAILED, Optional.empty(), AT, "/validations/id"));
        }

        @Test
        @DisplayName("without a validation")
        void withoutAValidation() {
            assertThrows(NullPointerException.class, () -> new ValidationCompleted("id", null,
                    ValidationJob.Status.FAILED, Optional.empty(), AT, "/validations/id"));
        }

        @Test
        @DisplayName("without a status")
        void withoutAStatus() {
            assertThrows(NullPointerException.class, () -> new ValidationCompleted("id", "name-iban-match",
                    null, Optional.empty(), AT, "/validations/id"));
        }

        @Test
        @DisplayName("without an optional outcome")
        void withoutAnOptionalOutcome() {
            assertThrows(NullPointerException.class, () -> new ValidationCompleted("id", "name-iban-match",
                    ValidationJob.Status.FAILED, null, AT, "/validations/id"));
        }

        @Test
        @DisplayName("without the moment it completed")
        void withoutTheMoment() {
            assertThrows(NullPointerException.class, () -> new ValidationCompleted("id", "name-iban-match",
                    ValidationJob.Status.FAILED, Optional.empty(), null, "/validations/id"));
        }

        @Test
        @DisplayName("without a location")
        void withoutALocation() {
            assertThrows(NullPointerException.class, () -> new ValidationCompleted("id", "name-iban-match",
                    ValidationJob.Status.FAILED, Optional.empty(), AT, null));
        }
    }
}
