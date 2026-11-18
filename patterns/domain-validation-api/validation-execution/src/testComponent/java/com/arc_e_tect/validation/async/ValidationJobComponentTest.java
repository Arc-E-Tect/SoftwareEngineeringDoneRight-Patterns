package com.arc_e_tect.validation.async;

import com.arc_e_tect.validation.async.adapter.outbound.memory.InMemoryValidationJobStore;
import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.domain.model.Outcome;
import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;
import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import com.arc_e_tect.validation.async.application.domain.service.GetValidationJobService;
import com.arc_e_tect.validation.async.application.domain.service.SubmitValidationJobService;
import com.arc_e_tect.validation.async.application.port.inbound.GetValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.SubmitValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.command.GetValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.ValidationOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * A validation job's life as one component: the use cases that submit and find jobs, with the
 * in-memory job store, from submission to the completion that is published. The executor holds the
 * jobs until a test runs them, and the publisher records what it is told, so that each step is seen.
 */
@DisplayName("A validation job, from submission to the completion that is published")
class ValidationJobComponentTest {

    private static final String VALIDATION = "name-iban-match";
    private static final String OPERATION = "/payments/v1/validations/name-iban-match";
    private static final String KEY = "3f1c9b0e-6a2d-4f7e-9c41-0b8a5e2d7c11";
    private static final Instant NOW = Instant.parse("2026-11-18T10:35:02Z");
    private static final JobProblem UNEXPECTED = new JobProblem(URI.create("https://api.example.com/problems/internal-server-error"),
            "Internal Server Error", 500, "An unexpected error occurred while running the validation.");

    private final InMemoryValidationJobStore store = new InMemoryValidationJobStore();
    private final List<Runnable> queued = new ArrayList<>();
    private final List<ValidationCompleted> published = new ArrayList<>();
    private final SubmitValidationJobUseCase submit = new SubmitValidationJobService(store, queued::add, Clock.fixed(NOW, ZoneOffset.UTC),
            List.of(published::add), UNEXPECTED);
    private final GetValidationJobUseCase get = new GetValidationJobService(store);

    private SubmitValidationJobResult submit(Object request, Supplier<JobOutcome> work) {
        return submit.submit(new SubmitValidationJobCommand(VALIDATION, KEY, request, OPERATION, work));
    }

    private static ValidationJobView accepted(SubmitValidationJobResult result) {
        return assertInstanceOf(SubmitValidationJobResult.Accepted.class, result).job();
    }

    private ValidationJobView find(ValidationJobView job) {
        return get.find(new GetValidationJobCommand(VALIDATION, job.validationId())).orElseThrow();
    }

    private void runTheJobs() {
        List<Runnable> jobs = List.copyOf(queued);
        queued.clear();
        jobs.forEach(Runnable::run);
    }

    @Test
    @DisplayName("is pending once submitted, completed with its result once it has run, and its completion is published")
    void completes() {
        ValidationJobView job = accepted(submit("request", () -> new JobOutcome.Completed("result", ValidationOutcome.INVALID)));

        assertEquals(ValidationJobView.Status.PENDING, find(job).status());
        assertEquals(List.of(), published);

        runTheJobs();

        assertEquals(List.of(ValidationJobView.Status.COMPLETED, Optional.of("result")), List.of(find(job).status(), find(job).result()));
        assertEquals(List.of(new ValidationCompleted(job.validationId(), VALIDATION, ValidationJob.Status.COMPLETED, Optional.of(Outcome.INVALID),
                NOW, OPERATION + "/" + job.validationId())), published);
    }

    @Test
    @DisplayName("fails with the unexpected-failure problem when its validation throws, and that is published too")
    void failsUnexpectedly() {
        ValidationJobView job = accepted(submit("request", () -> {
            throw new IllegalStateException("broken");
        }));

        runTheJobs();

        assertEquals(List.of(ValidationJobView.Status.FAILED, Optional.of(new ValidationJobView.Problem(UNEXPECTED.type(), UNEXPECTED.title(),
                UNEXPECTED.status(), UNEXPECTED.detail()))), List.of(find(job).status(), find(job).problem()));
        assertEquals(List.of(ValidationJob.Status.FAILED), published.stream().map(ValidationCompleted::status).toList());
    }

    @Test
    @DisplayName("is the same job, run once, when submitted again with its key and an equal request")
    void isSubmittedOnce() {
        ValidationJobView first = accepted(submit("request", () -> new JobOutcome.Completed("result", ValidationOutcome.VALID)));
        runTheJobs();

        ValidationJobView again = accepted(submit("request", () -> new JobOutcome.Completed("other", ValidationOutcome.VALID)));
        runTheJobs();

        assertEquals(List.of(first.validationId(), ValidationJobView.Status.COMPLETED, Optional.of("result"), 1),
                List.of(again.validationId(), again.status(), again.result(), published.size()));
    }

    @Test
    @DisplayName("is refused when its key is used again with a different request")
    void refusesAReusedKey() {
        accepted(submit("request", () -> new JobOutcome.Completed("result", ValidationOutcome.VALID)));

        assertEquals(new SubmitValidationJobResult.IdempotencyKeyReused(KEY),
                submit("another request", () -> new JobOutcome.Completed("result", ValidationOutcome.VALID)));
    }

    @Test
    @DisplayName("is not found under another validation")
    void isNotFoundUnderAnotherValidation() {
        ValidationJobView job = accepted(submit("request", () -> new JobOutcome.Completed("result", ValidationOutcome.VALID)));

        assertEquals(Optional.empty(), get.find(new GetValidationJobCommand("another-validation", job.validationId())));
    }
}
