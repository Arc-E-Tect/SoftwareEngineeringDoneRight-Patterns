package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchApi;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchJobV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchRequestV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchResultV1;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.ValidateNameIbanMatchUseCase;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * The inbound web adapter of name-iban-match: it implements the interface generated from the
 * contract, so its mappings, and the validation of the request, come from the contract.
 *
 * <p>It maps the request to the use case's command, and each way the use case can end to the
 * contract's answer: a verdict to {@code 200}, a request the domain refuses to {@code 422}.
 * Until the asynchronous execution exists, every validation is answered synchronously: a server
 * may ignore a preference it does not support (RFC 7240), and no validation job exists.
 */
@RestController
public class NameIbanMatchController implements NameIbanMatchApi {

    private final ValidateNameIbanMatchUseCase useCase;
    private final NameIbanMatchMapper mapper;

    public NameIbanMatchController(ValidateNameIbanMatchUseCase useCase, NameIbanMatchMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<NameIbanMatchResultV1> validateNameIbanMatch(
            NameIbanMatchRequestV1 request, String prefer, String idempotencyKey) {
        return switch (useCase.validate(command(request))) {
            case ValidateNameIbanMatchResult.Checked checked ->
                    ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mapper.toResult(checked));
            case ValidateNameIbanMatchResult.UnsupportedCountry refused ->
                    throw NameIbanMatchProblems.unsupportedCountry(refused.countryCode());
            case ValidateNameIbanMatchResult.InvalidChecksum refused ->
                    throw NameIbanMatchProblems.invalidChecksum();
        };
    }

    @Override
    public ResponseEntity<NameIbanMatchJobV1> getNameIbanMatchJob(String validationId) {
        throw NameIbanMatchProblems.jobNotFound(validationId);
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
