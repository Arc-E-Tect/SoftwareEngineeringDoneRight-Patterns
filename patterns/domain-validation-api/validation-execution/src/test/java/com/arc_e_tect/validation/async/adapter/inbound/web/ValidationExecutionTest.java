package com.arc_e_tect.validation.async.adapter.inbound.web;

import com.arc_e_tect.validation.async.application.port.inbound.command.GetValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.ValidationOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("The execution of a validation")
class ValidationExecutionTest {

    private static final String KEY = "3f1c9b0e-6a2d-4f7e-9c41-0b8a5e2d7c11";
    private static final String ID = "8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f";
    private static final String OPERATION = "/payments/v1/validations/name-iban-match";
    private static final String LOCATION = "/jobs/" + ID;
    private static final ValidationJobView PENDING = new ValidationJobView(ID, "name-iban-match", LOCATION,
            ValidationJobView.Status.PENDING, Optional.empty(), Optional.empty());
    private static final ValidationJobView COMPLETED = new ValidationJobView(ID, "name-iban-match", LOCATION,
            ValidationJobView.Status.COMPLETED, Optional.of("result"), Optional.empty());

    private final ValidationExecutionProblems problems = new ValidationExecutionProblems(URI.create("https://api.example.com/problems/"));
    private final List<SubmitValidationJobCommand> submitted = new ArrayList<>();
    private final List<GetValidationJobCommand> asked = new ArrayList<>();
    private SubmitValidationJobResult submitResult = new SubmitValidationJobResult.Accepted(PENDING);
    private Optional<ValidationJobView> found = Optional.of(PENDING);

    private final ValidationExecution execution = new ValidationExecution(
            command -> {
                submitted.add(command);
                return submitResult;
            },
            command -> {
                asked.add(command);
                return found;
            },
            problems, Duration.ofSeconds(2));

    @BeforeEach
    void receiveARequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", OPERATION)));
    }

    @AfterEach
    void forgetTheRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private ResponseEntity<String> run(String prefer, String key) {
        return execution.run("name-iban-match", prefer, key, "request", () -> "result", result -> ValidationOutcome.INDETERMINATE);
    }

    /** The problem a call answers with; whether it answers with one at all is another test's concern. */
    private static ErrorResponseException refusal(Runnable call) {
        try {
            call.run();
        } catch (ErrorResponseException refused) {
            return refused;
        }
        throw new IllegalStateException("The call answered without a problem");
    }

    @Nested
    @DisplayName("without a preference for an asynchronous answer")
    class Synchronously {

        @ParameterizedTest(name = "Prefer: {0}")
        @ValueSource(strings = {"return=minimal", "wait=10", "respond-asynchronously"})
        @DisplayName("answers 200, whatever else is preferred")
        void answers200(String prefer) {
            assertEquals(HttpStatus.OK, run(prefer, KEY).getStatusCode());
        }

        @Test
        @DisplayName("answers 200 without a Prefer header")
        void answers200WithoutPrefer() {
            assertEquals(HttpStatus.OK, run(null, null).getStatusCode());
        }

        @Test
        @DisplayName("answers with the validation's result")
        void answersWithTheResult() {
            assertEquals("result", run(null, null).getBody());
        }

        @Test
        @DisplayName("answers with a response that is not stored")
        void answersNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), run(null, null).getHeaders().getCacheControl());
        }

        @Test
        @DisplayName("submits no job, even with an Idempotency-Key")
        void submitsNoJob() {
            run(null, KEY);

            assertEquals(List.of(), submitted);
        }
    }

    @Nested
    @DisplayName("with a preference for an asynchronous answer")
    class Asynchronously {

        @ParameterizedTest(name = "Prefer: {0}")
        @ValueSource(strings = {"respond-async", "RESPOND-ASYNC", "wait=10, respond-async", "return=minimal,respond-async ; x=1"})
        @DisplayName("answers 202")
        void answers202(String prefer) {
            assertEquals(HttpStatus.ACCEPTED, run(prefer, KEY).getStatusCode());
        }

        @Test
        @DisplayName("submits the job with the validation, the key, the request and the operation's path")
        void submitsTheJob() {
            run("respond-async", KEY);

            assertEquals(List.of("name-iban-match", KEY, "request", OPERATION),
                    List.of(submitted.getFirst().validation(), submitted.getFirst().idempotencyKey(), submitted.getFirst().request(),
                            submitted.getFirst().operationPath()));
        }

        @Test
        @DisplayName("submits a job that completes with the validation's result and its outcome")
        void submitsAJobThatCompletes() {
            run("respond-async", KEY);

            assertEquals(new JobOutcome.Completed("result", ValidationOutcome.INDETERMINATE), submitted.getFirst().work().get());
        }

        @Test
        @DisplayName("submits a job that fails with the problem the validation answers with")
        void submitsAJobThatFails() {
            ProblemDetail refused = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), "Accounts in BR cannot be verified.");
            refused.setType(URI.create("https://api.example.com/payments/problems/unsupported-country"));
            refused.setTitle("Unsupported Country");
            execution.run("name-iban-match", "respond-async", KEY, "request", () -> {
                throw new ErrorResponseException(HttpStatusCode.valueOf(422), refused, null);
            }, result -> ValidationOutcome.VALID);

            assertEquals(new JobOutcome.Failed(URI.create("https://api.example.com/payments/problems/unsupported-country"),
                    "Unsupported Country", 422, "Accounts in BR cannot be verified."), submitted.getFirst().work().get());
        }

        @Test
        @DisplayName("answers with the job's location")
        void answersWithTheLocation() {
            assertEquals(URI.create(LOCATION), run("respond-async", KEY).getHeaders().getLocation());
        }

        @Test
        @DisplayName("answers when to read the job")
        void answersWhenToReadTheJob() {
            assertEquals("2", run("respond-async", KEY).getHeaders().getFirst("Retry-After"));
        }

        @Test
        @DisplayName("answers with a response that is not stored")
        void answersNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), run("respond-async", KEY).getHeaders().getCacheControl());
        }

        @Test
        @DisplayName("answers with the job")
        void answersWithTheJob() {
            ResponseEntity<?> response = run("respond-async", KEY);

            assertEquals(new AcceptedValidationJob(ID, "name-iban-match", "pending"), response.getBody());
        }

        @Test
        @DisplayName("answers with the job as it is, for a job submitted before")
        void answersWithTheJobAsItIs() {
            submitResult = new SubmitValidationJobResult.Accepted(COMPLETED);
            ResponseEntity<?> response = run("respond-async", KEY);

            assertEquals(new AcceptedValidationJob(ID, "name-iban-match", "completed"), response.getBody());
        }

        @Test
        @DisplayName("answers the invalid-request problem without an Idempotency-Key")
        void refusesWithoutAKey() {
            assertEquals(problems.missingIdempotencyKey().getBody(), refusal(() -> run("respond-async", null)).getBody());
        }

        @Test
        @DisplayName("answers the idempotency-key-reused problem for a key used with another request")
        void refusesAReusedKey() {
            submitResult = new SubmitValidationJobResult.IdempotencyKeyReused(KEY);

            assertEquals(problems.idempotencyKeyReused(KEY).getBody(), refusal(() -> run("respond-async", KEY)).getBody());
        }
    }

    @Nested
    @DisplayName("for the job resource")
    class ForTheJobResource {

        private ResponseEntity<String> job() {
            return execution.job("name-iban-match", ID, view -> view.status().name());
        }

        @Test
        @DisplayName("asks for the job of the validation")
        void asksForTheJob() {
            job();

            assertEquals(List.of(new GetValidationJobCommand("name-iban-match", ID)), asked);
        }

        @Test
        @DisplayName("answers 200 with the job, as the validation maps it")
        void answersWithTheMappedJob() {
            assertEquals(ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Retry-After", "2").body("PENDING"), job());
        }

        @Test
        @DisplayName("answers without Retry-After once the job is no longer pending")
        void answersWithoutRetryAfterOnceDone() {
            found = Optional.of(COMPLETED);

            assertNull(job().getHeaders().getFirst("Retry-After"));
        }

        @Test
        @DisplayName("answers the validation-job-not-found problem for a job it does not find")
        void answersNotFound() {
            found = Optional.empty();

            assertEquals(problems.jobNotFound("name-iban-match", ID).getBody(), refusal(this::job).getBody());
        }
    }
}
