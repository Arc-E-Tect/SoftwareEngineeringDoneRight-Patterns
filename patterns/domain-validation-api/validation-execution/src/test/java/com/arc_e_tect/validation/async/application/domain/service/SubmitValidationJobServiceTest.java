package com.arc_e_tect.validation.async.application.domain.service;

import com.arc_e_tect.validation.async.adapter.outbound.memory.InMemoryValidationJobStore;
import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The service that submits validation jobs")
class SubmitValidationJobServiceTest {

    private static final String KEY = "3f1c9b0e-6a2d-4f7e-9c41-0b8a5e2d7c11";
    private static final JobProblem UNEXPECTED = new JobProblem(URI.create("https://api.example.com/problems/internal-server-error"),
            "Internal Server Error", 500, "An unexpected error occurred while running the validation.");

    private final InMemoryValidationJobStore store = new InMemoryValidationJobStore();
    /** Runs nothing until it is told to, so that a test sees the job before and after it ran. */
    private final List<Runnable> queued = new ArrayList<>();
    private final SubmitValidationJobService service = new SubmitValidationJobService(store, queued::add, UNEXPECTED);

    private SubmitValidationJobResult submit(String key, Object request, Supplier<JobOutcome> work) {
        return service.submit(new SubmitValidationJobCommand("name-iban-match", key, request, work));
    }

    private ValidationJobView accepted(SubmitValidationJobResult result) {
        return ((SubmitValidationJobResult.Accepted) result).job();
    }

    private ValidationJob stored(ValidationJobView job) {
        return store.find("name-iban-match", job.validationId()).orElseThrow();
    }

    private ValidationJob run(Supplier<JobOutcome> work) {
        ValidationJobView job = accepted(submit(KEY, "request", work));
        queued.forEach(Runnable::run);
        return stored(job);
    }

    @Nested
    @DisplayName("for a new idempotency key")
    class ForANewKey {

        @Test
        @DisplayName("accepts a pending job")
        void acceptsAPendingJob() {
            assertEquals(ValidationJobView.Status.PENDING, accepted(submit(KEY, "request", () -> new JobOutcome.Completed("result"))).status());
        }

        @Test
        @DisplayName("accepts the job for its validation")
        void acceptsTheJobForItsValidation() {
            assertEquals("name-iban-match", accepted(submit(KEY, "request", () -> new JobOutcome.Completed("result"))).validation());
        }

        @Test
        @DisplayName("stores the job before it runs")
        void storesTheJobBeforeItRuns() {
            ValidationJobView job = accepted(submit(KEY, "request", () -> new JobOutcome.Completed("result")));

            assertEquals(ValidationJob.Status.PENDING, stored(job).status());
        }

        @Test
        @DisplayName("runs the job once, in the background")
        void runsTheJobOnceInTheBackground() {
            submit(KEY, "request", () -> new JobOutcome.Completed("result"));

            assertEquals(1, queued.size());
        }

        @Test
        @DisplayName("completes the job with its result")
        void completesTheJob() {
            assertEquals(Optional.of("result"), run(() -> new JobOutcome.Completed("result")).result());
        }

        @Test
        @DisplayName("fails the job with the problem the validation answered")
        void failsTheJobWithTheValidationsProblem() {
            URI type = URI.create("https://api.example.com/payments/problems/unsupported-country");

            assertEquals(Optional.of(new JobProblem(type, "Unsupported Country", 422, "Accounts in BR cannot be verified.")),
                    run(() -> new JobOutcome.Failed(type, "Unsupported Country", 422, "Accounts in BR cannot be verified.")).problem());
        }

        @Test
        @DisplayName("fails the job with the unexpected-failure problem when the validation fails otherwise")
        void failsTheJobWhenTheValidationFails() {
            assertEquals(Optional.of(UNEXPECTED), run(() -> {
                throw new IllegalStateException("broken");
            }).problem());
        }
    }

    @Nested
    @DisplayName("for an idempotency key it has seen")
    class ForAKeyItHasSeen {

        private final ValidationJobView first = accepted(submit(KEY, "request", () -> new JobOutcome.Completed("result")));

        @Test
        @DisplayName("accepts the same job again for an equal request")
        void acceptsTheSameJobForAnEqualRequest() {
            assertEquals(first.validationId(),
                    accepted(submit(KEY, new String("request"), () -> new JobOutcome.Completed("result"))).validationId());
        }

        @Test
        @DisplayName("does not run the job again for an equal request")
        void doesNotRunTheJobAgain() {
            AtomicInteger runs = new AtomicInteger();
            submit(KEY, "request", () -> new JobOutcome.Completed(runs.incrementAndGet()));
            queued.forEach(Runnable::run);

            assertEquals(0, runs.get());
        }

        @Test
        @DisplayName("refuses a different request")
        void refusesADifferentRequest() {
            assertEquals(new SubmitValidationJobResult.IdempotencyKeyReused(KEY),
                    submit(KEY, "another request", () -> new JobOutcome.Completed("result")));
        }

        @Test
        @DisplayName("accepts another job for another key")
        void acceptsAnotherJobForAnotherKey() {
            ValidationJobView second = accepted(submit("8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f", "request",
                    () -> new JobOutcome.Completed("result")));

            assertNotEquals(first.validationId(), second.validationId());
        }
    }

    @Nested
    @DisplayName("is refused")
    class IsRefused {

        @Test
        @DisplayName("without a store")
        void withoutAStore() {
            assertThrows(NullPointerException.class, () -> new SubmitValidationJobService(null, queued::add, UNEXPECTED));
        }

        @Test
        @DisplayName("without an executor")
        void withoutAnExecutor() {
            assertThrows(NullPointerException.class, () -> new SubmitValidationJobService(store, null, UNEXPECTED));
        }

        @Test
        @DisplayName("without the unexpected-failure problem")
        void withoutTheUnexpectedFailureProblem() {
            assertThrows(NullPointerException.class, () -> new SubmitValidationJobService(store, queued::add, null));
        }
    }
}
