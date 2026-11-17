package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchRequestV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchResultV1;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.ValidateNameIbanMatchUseCase;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Checked;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Outcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The name-iban-match controller")
class NameIbanMatchControllerTest {

    private static final NameIbanMatchRequestV1 REQUEST = new NameIbanMatchRequestV1("NL91ABNA0417164300", "Jan de Vries");
    private static final Checked VALID = new Checked(Outcome.VALID, Optional.empty(), List.of());

    private final NameIbanMatchMapper mapper = new NameIbanMatchMapper();
    private final List<ValidateNameIbanMatchCommand> commands = new ArrayList<>();

    private NameIbanMatchController answering(ValidateNameIbanMatchResult result) {
        ValidateNameIbanMatchUseCase useCase = command -> {
            commands.add(command);
            return result;
        };
        return new NameIbanMatchController(useCase, mapper);
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
    @DisplayName("when the rule was checked")
    class WhenChecked {

        private final ResponseEntity<NameIbanMatchResultV1> response = answering(VALID).validateNameIbanMatch(REQUEST, null, null);

        @Test
        @DisplayName("asks the use case with the request's command")
        void asksTheUseCase() {
            assertEquals(List.of(new ValidateNameIbanMatchCommand("NL91ABNA0417164300", "Jan de Vries")), commands);
        }

        @Test
        @DisplayName("answers 200")
        void answers200() {
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("answers with the contract's result")
        void answersWithTheResult() {
            assertEquals(mapper.toResult(VALID), response.getBody());
        }

        @Test
        @DisplayName("answers with a response that is not stored")
        void answersNotStored() {
            assertEquals(CacheControl.noStore().getHeaderValue(), response.getHeaders().getCacheControl());
        }
    }

    @Nested
    @DisplayName("when the domain refuses the request")
    class WhenRefused {

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
        @DisplayName("answers with the invalid-request problem")
        void answersWithTheInvalidRequestProblem() {
            NameIbanMatchController controller = answering(VALID);

            assertEquals(HttpStatus.BAD_REQUEST, refusal(() -> controller.validateNameIbanMatch(emSpaceOnly, null, null)).getStatusCode());
        }

        @Test
        @DisplayName("does not ask the use case")
        void doesNotAskTheUseCase() {
            NameIbanMatchController controller = answering(VALID);
            refusal(() -> controller.validateNameIbanMatch(emSpaceOnly, null, null));

            assertEquals(List.of(), commands);
        }
    }

    @Test
    @DisplayName("answers that no validation job exists, as long as there is no asynchronous execution")
    void answersThatNoJobExists() {
        NameIbanMatchController controller = answering(VALID);

        assertEquals(NameIbanMatchProblems.jobNotFound("8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f").getBody(),
                refusal(() -> controller.getNameIbanMatchJob("8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f")).getBody());
    }
}
