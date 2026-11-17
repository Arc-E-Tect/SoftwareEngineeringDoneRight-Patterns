package com.arc_e_tect.validation.async.configuration;

import com.arc_e_tect.validation.async.adapter.inbound.web.ValidationExecution;
import com.arc_e_tect.validation.async.adapter.outbound.memory.InMemoryValidationJobStore;
import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.port.inbound.GetValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.SubmitValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.command.GetValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DisplayName("The auto-configuration of the asynchronous execution")
class ValidationExecutionAutoConfigurationTest {

    private final ValidationExecutionAutoConfiguration configuration = new ValidationExecutionAutoConfiguration();
    private final ValidationExecutionProperties properties =
            new ValidationExecutionProperties(Duration.ofSeconds(3), URI.create("https://problems.example.org/"));
    private final InMemoryValidationJobStore store = new InMemoryValidationJobStore();

    /** The problem a call answers with; whether it answers with one at all is another test's concern. */
    private static ErrorResponseException refusal(Runnable call) {
        try {
            call.run();
        } catch (ErrorResponseException refused) {
            return refused;
        }
        throw new IllegalStateException("The call answered without a problem");
    }

    @AfterEach
    void forgetTheRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("stores jobs in memory, unless the bounded context supplies a store")
    void storesJobsInMemory() {
        assertInstanceOf(InMemoryValidationJobStore.class, configuration.validationJobStore());
    }

    @Test
    @DisplayName("fails a job that fails unexpectedly with the internal-server-error problem of the configured base")
    void failsAnUnexpectedFailureWithTheInternalServerErrorProblem() {
        assertEquals(new JobProblem(URI.create("https://problems.example.org/internal-server-error"), "Internal Server Error", 500,
                "An unexpected error occurred while running the validation."), configuration.unexpectedFailure(properties));
    }

    @Test
    @DisplayName("submits jobs to the store")
    void submitsJobsToTheStore() {
        SubmitValidationJobUseCase submit = configuration.submitValidationJobUseCase(store, properties);
        ValidationJobView job = ((SubmitValidationJobResult.Accepted) submit.submit(new SubmitValidationJobCommand(
                "name-iban-match", "key", "request", () -> new JobOutcome.Completed("result")))).job();

        assertEquals(job.validationId(), store.find("name-iban-match", job.validationId()).orElseThrow().validationId());
    }

    @Test
    @DisplayName("finds jobs in the store")
    void findsJobsInTheStore() {
        GetValidationJobUseCase get = configuration.getValidationJobUseCase(store);

        assertEquals(Optional.empty(), get.find(new GetValidationJobCommand("name-iban-match", "id")));
    }

    @Test
    @DisplayName("answers Retry-After as configured")
    void answersRetryAfterAsConfigured() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/payments/v1/validations/name-iban-match")));
        ValidationExecution execution = configuration.validationExecution(
                configuration.submitValidationJobUseCase(store, properties), configuration.getValidationJobUseCase(store), properties);

        ResponseEntity<String> response = execution.run("name-iban-match", "respond-async", "key", "request", () -> "result");

        assertEquals("3", response.getHeaders().getFirst("Retry-After"));
    }

    @Test
    @DisplayName("answers problems of the configured base")
    void answersProblemsOfTheConfiguredBase() {
        ValidationExecution execution = configuration.validationExecution(
                configuration.submitValidationJobUseCase(store, properties), configuration.getValidationJobUseCase(store), properties);

        ErrorResponseException notFound = refusal(() -> execution.job("name-iban-match", "id", view -> view));

        assertEquals(URI.create("https://problems.example.org/validation-job-not-found"), notFound.getBody().getType());
    }
}
