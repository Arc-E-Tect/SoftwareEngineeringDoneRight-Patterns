package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchApi;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchJobV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchRequestV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchResultV1;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.ValidateNameIbanMatchUseCase;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;
import com.arc_e_tect.validation.async.adapter.inbound.web.ValidationExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * The inbound web adapter of name-iban-match: it implements the interface generated from the
 * contract, so its mappings, and the validation of the request, come from the contract.
 *
 * <p>It maps the request to the use case's command, and each way the use case can end to the
 * contract's answer: a verdict to its result, a request the domain refuses to its problem. Whether
 * the validation is answered at once or runs as a job, and everything about the job, is the
 * asynchronous execution's: the controller hands it the work and how to read a result's outcome, and
 * maps the job to the contract's.
 */
@RestController
public class NameIbanMatchController implements NameIbanMatchApi {

    private static final String VALIDATION = "name-iban-match";

    private final ValidateNameIbanMatchUseCase useCase;
    private final NameIbanMatchMapper mapper;
    private final ValidationExecution execution;

    public NameIbanMatchController(ValidateNameIbanMatchUseCase useCase, NameIbanMatchMapper mapper, ValidationExecution execution) {
        this.useCase = useCase;
        this.mapper = mapper;
        this.execution = execution;
    }

    @Override
    public ResponseEntity<NameIbanMatchResultV1> validateNameIbanMatch(
            NameIbanMatchRequestV1 request, String prefer, String idempotencyKey) {
        ValidateNameIbanMatchCommand command = command(request);
        return execution.run(VALIDATION, prefer, idempotencyKey, command,
                () -> switch (useCase.validate(command)) {
                    case ValidateNameIbanMatchResult.Checked checked -> mapper.toResult(checked);
                    case ValidateNameIbanMatchResult.UnsupportedCountry refused ->
                            throw NameIbanMatchProblems.unsupportedCountry(refused.countryCode());
                    case ValidateNameIbanMatchResult.InvalidChecksum refused ->
                            throw NameIbanMatchProblems.invalidChecksum();
                },
                mapper::outcomeOf);
    }

    @Override
    public ResponseEntity<NameIbanMatchJobV1> getNameIbanMatchJob(String validationId) {
        return execution.job(VALIDATION, validationId, mapper::toJob);
    }

    /**
     * The command refuses only what the contract refuses. Java's {@code \s} is narrower than the
     * contract's, so a name of only, say, em spaces passes the generated validation; the command
     * refuses it, and that is the contract's 400.
     */
    private ValidateNameIbanMatchCommand command(NameIbanMatchRequestV1 request) {
        try {
            return mapper.toCommand(request);
        } catch (IllegalArgumentException refused) {
            throw NameIbanMatchProblems.invalidRequest(refused.getMessage());
        }
    }
}
