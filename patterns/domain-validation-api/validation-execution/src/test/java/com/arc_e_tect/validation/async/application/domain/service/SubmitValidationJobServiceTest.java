package com.arc_e_tect.validation.async.application.domain.service;

import com.arc_e_tect.validation.async.adapter.outbound.memory.InMemoryValidationJobStore;
import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.domain.model.Outcome;
import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;
import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.ValidationOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationCompletedPublisher;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
    private static final Instant NOW = Instant.parse("2026-11-17T10:35:02Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String OPERATION = "/payments/v1/validations/name-iban-match";

    private final List<ValidationCompleted> published = new ArrayList<>();
    private final List<ValidationCompleted> publishedToo = new ArrayList<>();
    private final SubmitValidationJobService service = new SubmitValidationJobService(store, queued::add, CLOCK,
            List.of(published::add, publishedToo::add), UNEXPECTED);

    private static JobOutcome.Completed completed(Object result) {
        return new JobOutcome.Completed(result, ValidationOutcome.VALID);
    }

    private SubmitValidationJobResult submit(String key, Object request, Supplier<JobOutcome> work) {
        return service.submit(new SubmitValidationJobCommand("name-iban-match", key, request, OPERATION, work));
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
            assertEquals(ValidationJobView.Status.PENDING, accepted(submit(KEY, "request", () -> completed("result"))).status());
        }

        @Test
        @DisplayName("accepts the job for its validation")
        void acceptsTheJobForItsValidation() {
            assertEquals("name-iban-match", accepted(submit(KEY, "request", () -> completed("result"))).validation());
        }

        @Test
        @DisplayName("accepts the job at its location below the operation's path")
        void acceptsTheJobAtItsLocation() {
            ValidationJobView job = accepted(submit(KEY, "request", () -> completed("result")));

            assertEquals(OPERATION + "/" + job.validationId(), job.location());
        }

        @Test
        @DisplayName("stores the job before it runs")
        void storesTheJobBeforeItRuns() {
            ValidationJobView job = accepted(submit(KEY, "request", () -> completed("result")));

            assertEquals(ValidationJob.Status.PENDING, stored(job).status());
        }

        @Test
        @DisplayName("runs the job once, in the background")
        void runsTheJobOnceInTheBackground() {
            submit(KEY, "request", () -> completed("result"));

            assertEquals(1, queued.size());
        }

        @Test
        @DisplayName("completes the job with its result")
        void completesTheJob() {
            assertEquals(Optional.of("result"), run(() -> completed("result")).result());
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
    @DisplayName("once a job has run")
    class OnceAJobHasRun {

        private final ValidationCompletedPublisher broken = event -> {
            throw new IllegalStateException("The broker is unavailable");
        };

        @Test
        @DisplayName("publishes nothing before the job has run")
        void publishesNothingBeforeItRan() {
            submit(KEY, "request", () -> completed("result"));

            assertEquals(List.of(), published);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(ValidationOutcome.class)
        @DisplayName("publishes that the job completed, with the validation's outcome, when it ran")
        void publishesTheCompletion(ValidationOutcome outcome) {
            ValidationJob job = run(() -> new JobOutcome.Completed("result", outcome));

            assertEquals(List.of(new ValidationCompleted(job.validationId(), "name-iban-match", ValidationJob.Status.COMPLETED,
                    Optional.of(Outcome.valueOf(outcome.name())), NOW, job.location())), published);
        }

        @Test
        @DisplayName("publishes that the job failed, without an outcome")
        void publishesTheFailure() {
            ValidationJob job = run(() -> new JobOutcome.Failed(URI.create("https://api.example.com/problems/x"), "X", 422, "x"));

            assertEquals(List.of(new ValidationCompleted(job.validationId(), "name-iban-match", ValidationJob.Status.FAILED,
                    Optional.empty(), NOW, job.location())), published);
        }

        @Test
        @DisplayName("publishes that the job failed when the validation failed unexpectedly")
        void publishesTheUnexpectedFailure() {
            run(() -> {
                throw new IllegalStateException("broken");
            });

            assertEquals(ValidationJob.Status.FAILED, published.getFirst().status());
        }

        @Test
        @DisplayName("publishes to every publisher")
        void publishesToEveryPublisher() {
            run(() -> completed("result"));

            assertEquals(published, publishedToo);
        }

        @Test
        @DisplayName("publishes after the job is stored, so that a subscriber finds it as it ended")
        void publishesAfterStoring() {
            List<ValidationJob.Status> statusWhenPublished = new ArrayList<>();
            SubmitValidationJobService watched = new SubmitValidationJobService(store, queued::add, CLOCK,
                    List.of(event -> statusWhenPublished.add(store.find("name-iban-match", event.validationId()).orElseThrow().status())),
                    UNEXPECTED);
            watched.submit(new SubmitValidationJobCommand("name-iban-match", KEY, "request", OPERATION, () -> completed("result")));
            queued.forEach(Runnable::run);

            assertEquals(List.of(ValidationJob.Status.COMPLETED), statusWhenPublished);
        }

        @Test
        @DisplayName("still publishes to the others when a publisher fails, as publishing is best effort")
        void publishesDespiteAFailingPublisher() {
            SubmitValidationJobService withABrokenPublisher = new SubmitValidationJobService(store, queued::add, CLOCK,
                    List.of(broken, published::add), UNEXPECTED);
            withABrokenPublisher.submit(new SubmitValidationJobCommand("name-iban-match", KEY, "request", OPERATION, () -> completed("result")));
            queued.forEach(Runnable::run);

            assertEquals(1, published.size());
        }
    }

    @Nested
    @DisplayName("logs what fails unexpectedly, once, with the job and without the request")
    class LogsWhatFailsUnexpectedly {

        private static final String REQUEST = "{\"accountHolderName\": \"Jan de Vries\"}";

        private final Logger logger = (Logger) LoggerFactory.getLogger(SubmitValidationJobService.class);
        private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

        @BeforeEach
        void listen() {
            logged.start();
            logger.addAppender(logged);
        }

        @AfterEach
        void stopListening() {
            logger.detachAppender(logged);
        }

        private String runWith(List<ValidationCompletedPublisher> publishers, Supplier<JobOutcome> work) {
            SubmitValidationJobService submitting = new SubmitValidationJobService(store, queued::add, CLOCK, publishers, UNEXPECTED);
            String validationId = accepted(submitting.submit(new SubmitValidationJobCommand("name-iban-match", KEY, REQUEST, OPERATION, work)))
                    .validationId();
            queued.forEach(Runnable::run);
            return validationId;
        }

        private static List<Object> logEntry(Level level, String message, String exception) {
            return List.of(level, message, exception);
        }

        private List<Object> theOnlyEntry() {
            assertEquals(1, logged.list.size(), "log entries");
            ILoggingEvent event = logged.list.getFirst();
            return logEntry(event.getLevel(), event.getFormattedMessage(), event.getThrowableProxy().getClassName());
        }

        @Test
        @DisplayName("an error for a validation that fails")
        void aValidationThatFails() {
            String validationId = runWith(List.of(), () -> {
                throw new IllegalStateException("broken");
            });

            assertEquals(logEntry(Level.ERROR, "The name-iban-match validation of job " + validationId + " failed unexpectedly; the job has failed",
                    IllegalStateException.class.getName()), theOnlyEntry());
        }

        @Test
        @DisplayName("a warning for a publisher that fails, naming the publisher")
        void aPublisherThatFails() {
            ValidationCompletedPublisher broken = new BrokenPublisher();
            String validationId = runWith(List.of(broken), () -> completed("result"));

            assertEquals(logEntry(Level.WARN, "Publisher " + BrokenPublisher.class.getName() + " failed to publish the completion of job "
                    + validationId, IllegalStateException.class.getName()), theOnlyEntry());
        }

        @Test
        @DisplayName("nothing for a job that runs and is published")
        void nothingWhenAllIsWell() {
            runWith(List.of(published::add), () -> completed("result"));

            assertEquals(List.of(), logged.list);
        }

        private static final class BrokenPublisher implements ValidationCompletedPublisher {

            @Override
            public void publish(ValidationCompleted completed) {
                throw new IllegalStateException("The broker is unavailable");
            }
        }
    }

    @Nested
    @DisplayName("for an idempotency key it has seen")
    class ForAKeyItHasSeen {

        private final ValidationJobView first = accepted(submit(KEY, "request", () -> completed("result")));

        @Test
        @DisplayName("accepts the same job again for an equal request")
        void acceptsTheSameJobForAnEqualRequest() {
            assertEquals(first.validationId(),
                    accepted(submit(KEY, new String("request"), () -> completed("result"))).validationId());
        }

        @Test
        @DisplayName("does not run the job again for an equal request")
        void doesNotRunTheJobAgain() {
            AtomicInteger runs = new AtomicInteger();
            submit(KEY, "request", () -> completed(runs.incrementAndGet()));
            queued.forEach(Runnable::run);

            assertEquals(0, runs.get());
        }

        @Test
        @DisplayName("refuses a different request")
        void refusesADifferentRequest() {
            assertEquals(new SubmitValidationJobResult.IdempotencyKeyReused(KEY),
                    submit(KEY, "another request", () -> completed("result")));
        }

        @Test
        @DisplayName("accepts another job for another key")
        void acceptsAnotherJobForAnotherKey() {
            ValidationJobView second = accepted(submit("8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f", "request",
                    () -> completed("result")));

            assertNotEquals(first.validationId(), second.validationId());
        }
    }

    @Nested
    @DisplayName("is refused")
    class IsRefused {

        @Test
        @DisplayName("without a store")
        void withoutAStore() {
            assertThrows(NullPointerException.class, () -> new SubmitValidationJobService(null, queued::add, CLOCK, List.of(), UNEXPECTED));
        }

        @Test
        @DisplayName("without an executor")
        void withoutAnExecutor() {
            assertThrows(NullPointerException.class, () -> new SubmitValidationJobService(store, null, CLOCK, List.of(), UNEXPECTED));
        }

        @Test
        @DisplayName("without the unexpected-failure problem")
        void withoutTheUnexpectedFailureProblem() {
            assertThrows(NullPointerException.class, () -> new SubmitValidationJobService(store, queued::add, CLOCK, List.of(), null));
        }

        @Test
        @DisplayName("without a clock")
        void withoutAClock() {
            assertThrows(NullPointerException.class, () -> new SubmitValidationJobService(store, queued::add, null, List.of(), UNEXPECTED));
        }

        @Test
        @DisplayName("without its publishers")
        void withoutItsPublishers() {
            assertThrows(NullPointerException.class, () -> new SubmitValidationJobService(store, queued::add, CLOCK, null, UNEXPECTED));
        }
    }
}
