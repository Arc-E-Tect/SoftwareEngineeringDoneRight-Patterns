package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchJobV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchRequestV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchResultV1;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.ValidateNameIbanMatchUseCase;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Checked;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Outcome;
import com.arc_e_tect.validation.async.adapter.inbound.web.ValidationExecution;
import com.arc_e_tect.validation.async.adapter.inbound.web.ValidationExecutionProblems;
import com.arc_e_tect.validation.async.application.port.inbound.command.GetValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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

@DisplayName("The name-iban-match controller")
class NameIbanMatchControllerTest {

    private static final NameIbanMatchRequestV1 REQUEST = new NameIbanMatchRequestV1("NL91ABNA0417164300", "Jan de Vries");
    private static final ValidateNameIbanMatchCommand COMMAND = new ValidateNameIbanMatchCommand("NL91ABNA0417164300", "Jan de Vries");
    private static final Checked VALID = new Checked(Outcome.VALID, Optional.empty(), List.of());
    private static final String KEY = "3f1c9b0e-6a2d-4f7e-9c41-0b8a5e2d7c11";
    private static final String ID = "8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f";
    private static final ValidationJobView PENDING =
            new ValidationJobView(ID, "name-iban-match", ValidationJobView.Status.PENDING, Optional.empty(), Optional.empty());

    private final NameIbanMatchMapper mapper = new NameIbanMatchMapper();
    private final ValidationExecutionProblems problems = new ValidationExecutionProblems(URI.create("https://api.example.com/problems/"));
    private final List<ValidateNameIbanMatchCommand> commands = new ArrayList<>();
    private final List<SubmitValidationJobCommand> submitted = new ArrayList<>();
    private final List<GetValidationJobCommand> asked = new ArrayList<>();
    private Optional<ValidationJobView> found = Optional.of(PENDING);

    private final ValidationExecution execution = new ValidationExecution(
            command -> {
                submitted.add(command);
                return new SubmitValidationJobResult.Accepted(PENDING);
            },
            command -> {
                asked.add(command);
                return found;
            },
            problems, Duration.ofSeconds(1));

    @BeforeEach
    void receiveARequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/payments/v1/validations/name-iban-match")));
    }

    @AfterEach
    void forgetTheRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private NameIbanMatchController answering(ValidateNameIbanMatchResult result) {
        ValidateNameIbanMatchUseCase useCase = command -> {
            commands.add(command);
            return result;
        };
        return new NameIbanMatchController(useCase, mapper, execution);
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
    @DisplayName("synchronously, when the rule was checked")
    class SynchronouslyChecked {

        private final ResponseEntity<NameIbanMatchResultV1> response = answering(VALID).validateNameIbanMatch(REQUEST, null, null);

        @Test
        @DisplayName("asks the use case with the request's command")
        void asksTheUseCase() {
            assertEquals(List.of(COMMAND), commands);
        }

        @Test
        @DisplayName("answers 200 with the contract's result")
        void answersWithTheResult() {
            assertEquals(mapper.toResult(VALID), response.getBody());
        }
    }

    @Nested
    @DisplayName("synchronously, when the domain refuses the request")
    class SynchronouslyRefused {

        @Test
        @DisplayName("for its country, answers with the unsupported-country problem")
        void forItsCountry() {
            NameIbanMatchController controller = answering(new ValidateNameIbanMatchResult.UnsupportedCountry("BR"));

            assertEquals(NameIbanMatchProblems.unsupportedCountry("BR").getBody(),
                    refusal(() -> controller.validateNameIbanMatch(REQUEST, null, null)).getBody());
        }

        @Test
        @DisplayName("for its checksum, answers with the invalid-iban-checksum problem")
        void forItsChecksum() {
            NameIbanMatchController controller = answering(new ValidateNameIbanMatchResult.InvalidChecksum("NL92ABNA0417164300"));

            assertEquals(NameIbanMatchProblems.invalidChecksum().getBody(),
                    refusal(() -> controller.validateNameIbanMatch(REQUEST, null, null)).getBody());
        }
    }

    @Nested
    @DisplayName("when the command refuses the request, where Java checks less than the contract")
    class WhenTheCommandRefuses {

        private final NameIbanMatchRequestV1 emSpaceOnly = new NameIbanMatchRequestV1("NL91ABNA0417164300", " ");

        @Test
        @DisplayName("answers with the invalid-request problem, asynchronous or not")
        void answersWithTheInvalidRequestProblem() {
            NameIbanMatchController controller = answering(VALID);

            assertEquals(HttpStatus.BAD_REQUEST,
                    refusal(() -> controller.validateNameIbanMatch(emSpaceOnly, "respond-async", KEY)).getStatusCode());
        }

        @Test
        @DisplayName("neither asks the use case nor submits a job")
        void doesNotRunTheValidation() {
            NameIbanMatchController controller = answering(VALID);
            refusal(() -> controller.validateNameIbanMatch(emSpaceOnly, "respond-async", KEY));

            assertEquals(List.of(), List.of(commands, submitted).stream().flatMap(List::stream).toList());
        }
    }

    @Nested
    @DisplayName("asynchronously")
    class Asynchronously {

        @Test
        @DisplayName("answers 202")
        void answers202() {
            assertEquals(HttpStatus.ACCEPTED, answering(VALID).validateNameIbanMatch(REQUEST, "respond-async", KEY).getStatusCode());
        }

        @Test
        @DisplayName("submits a name-iban-match job under the key, for the request's command")
        void submitsTheJob() {
            answering(VALID).validateNameIbanMatch(REQUEST, "respond-async", KEY);

            assertEquals(List.of("name-iban-match", KEY, COMMAND),
                    List.of(submitted.getFirst().validation(), submitted.getFirst().idempotencyKey(), submitted.getFirst().request()));
        }

        @Test
        @DisplayName("submits a job that completes with the contract's result")
        void submitsAJobThatCompletes() {
            answering(VALID).validateNameIbanMatch(REQUEST, "respond-async", KEY);

            assertEquals(new JobOutcome.Completed(mapper.toResult(VALID)), submitted.getFirst().work().get());
        }

        @Test
        @DisplayName("submits a job that fails with the problem of a request the domain refuses")
        void submitsAJobThatFails() {
            answering(new ValidateNameIbanMatchResult.UnsupportedCountry("BR")).validateNameIbanMatch(REQUEST, "respond-async", KEY);

            assertEquals(new JobOutcome.Failed(URI.create("https://api.example.com/payments/problems/unsupported-country"),
                    "Unsupported Country", 422, "Accounts in BR cannot be verified."), submitted.getFirst().work().get());
        }
    }

    @Nested
    @DisplayName("for the job resource")
    class ForTheJobResource {

        @Test
        @DisplayName("asks for the name-iban-match job with the identifier")
        void asksForTheJob() {
            answering(VALID).getNameIbanMatchJob(ID);

            assertEquals(List.of(new GetValidationJobCommand("name-iban-match", ID)), asked);
        }

        @Test
        @DisplayName("answers with the job, as the contract types it")
        void answersWithTheJob() {
            ResponseEntity<NameIbanMatchJobV1> response = answering(VALID).getNameIbanMatchJob(ID);

            assertEquals(mapper.toJob(PENDING), response.getBody());
        }

        @Test
        @DisplayName("answers the validation-job-not-found problem for a job that does not exist")
        void answersNotFound() {
            found = Optional.empty();
            NameIbanMatchController controller = answering(VALID);

            assertEquals(problems.jobNotFound("name-iban-match", ID).getBody(), refusal(() -> controller.getNameIbanMatchJob(ID)).getBody());
        }
    }
}
