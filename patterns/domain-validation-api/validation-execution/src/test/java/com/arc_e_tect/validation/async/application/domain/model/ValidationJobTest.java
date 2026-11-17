package com.arc_e_tect.validation.async.application.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A validation job")
class ValidationJobTest {

    private static final String KEY = "3f1c9b0e-6a2d-4f7e-9c41-0b8a5e2d7c11";
    private static final JobProblem PROBLEM = new JobProblem(URI.create("https://api.example.com/problems/internal-server-error"),
            "Internal Server Error", 500, "An unexpected error occurred while running the validation.");

    private final ValidationJob pending = ValidationJob.pending("name-iban-match", KEY, "request");

    @Nested
    @DisplayName("when it is submitted")
    class WhenSubmitted {

        @Test
        @DisplayName("is pending")
        void isPending() {
            assertEquals(ValidationJob.Status.PENDING, pending.status());
        }

        @Test
        @DisplayName("has a UUID as its identifier")
        void hasAUuid() {
            assertEquals(pending.validationId(), UUID.fromString(pending.validationId()).toString());
        }

        @Test
        @DisplayName("has an identifier of its own")
        void hasAnIdentifierOfItsOwn() {
            assertNotEquals(pending.validationId(), ValidationJob.pending("name-iban-match", KEY, "request").validationId());
        }

        @Test
        @DisplayName("keeps its validation, idempotency key and request")
        void keepsWhatItRuns() {
            assertEquals(new ValidationJob(pending.validationId(), "name-iban-match", KEY, "request",
                    ValidationJob.Status.PENDING, Optional.empty(), Optional.empty()), pending);
        }
    }

    @Nested
    @DisplayName("when it has run")
    class WhenRun {

        @Test
        @DisplayName("is completed with its result")
        void isCompletedWithItsResult() {
            assertEquals(new ValidationJob(pending.validationId(), "name-iban-match", KEY, "request",
                    ValidationJob.Status.COMPLETED, Optional.of("result"), Optional.empty()), pending.completed("result"));
        }

        @Test
        @DisplayName("is failed with its problem")
        void isFailedWithItsProblem() {
            assertEquals(new ValidationJob(pending.validationId(), "name-iban-match", KEY, "request",
                    ValidationJob.Status.FAILED, Optional.empty(), Optional.of(PROBLEM)), pending.failed(PROBLEM));
        }

        @Test
        @DisplayName("cannot complete again")
        void cannotCompleteAgain() {
            ValidationJob completed = pending.completed("result");

            assertThrows(IllegalStateException.class, () -> completed.completed("another result"));
        }

        @Test
        @DisplayName("cannot fail after it completed")
        void cannotFailAfterItCompleted() {
            ValidationJob completed = pending.completed("result");

            assertThrows(IllegalStateException.class, () -> completed.failed(PROBLEM));
        }
    }

    @Nested
    @DisplayName("is refused")
    class IsRefused {

        @Test
        @DisplayName("with a result while it is not completed")
        void withAResultWhileNotCompleted() {
            assertThrows(IllegalArgumentException.class, () -> new ValidationJob("id", "name-iban-match", KEY, "request",
                    ValidationJob.Status.PENDING, Optional.of("result"), Optional.empty()));
        }

        @Test
        @DisplayName("completed without a result")
        void completedWithoutAResult() {
            assertThrows(IllegalArgumentException.class, () -> new ValidationJob("id", "name-iban-match", KEY, "request",
                    ValidationJob.Status.COMPLETED, Optional.empty(), Optional.empty()));
        }

        @Test
        @DisplayName("with a problem while it has not failed")
        void withAProblemWhileNotFailed() {
            assertThrows(IllegalArgumentException.class, () -> new ValidationJob("id", "name-iban-match", KEY, "request",
                    ValidationJob.Status.PENDING, Optional.empty(), Optional.of(PROBLEM)));
        }

        @Test
        @DisplayName("failed without a problem")
        void failedWithoutAProblem() {
            assertThrows(IllegalArgumentException.class, () -> new ValidationJob("id", "name-iban-match", KEY, "request",
                    ValidationJob.Status.FAILED, Optional.empty(), Optional.empty()));
        }

        @Test
        @DisplayName("without a request")
        void withoutARequest() {
            assertThrows(NullPointerException.class, () -> ValidationJob.pending("name-iban-match", KEY, null));
        }

        @Test
        @DisplayName("without an idempotency key")
        void withoutAnIdempotencyKey() {
            assertThrows(NullPointerException.class, () -> ValidationJob.pending("name-iban-match", null, "request"));
        }

        @Test
        @DisplayName("without a validation")
        void withoutAValidation() {
            assertThrows(NullPointerException.class, () -> ValidationJob.pending(null, KEY, "request"));
        }

        @Test
        @DisplayName("without an identifier")
        void withoutAnIdentifier() {
            assertThrows(NullPointerException.class, () -> new ValidationJob(null, "name-iban-match", KEY, "request",
                    ValidationJob.Status.PENDING, Optional.empty(), Optional.empty()));
        }

        @Test
        @DisplayName("without a status")
        void withoutAStatus() {
            assertThrows(NullPointerException.class, () -> new ValidationJob("id", "name-iban-match", KEY, "request",
                    null, Optional.empty(), Optional.empty()));
        }
    }
}
