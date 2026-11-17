package com.arc_e_tect.payments.contract;

import com.arc_e_tect.payments.contract.restdocs.GetNameIbanMatchJobContractTests;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchResultV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.OutcomeV1;
import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationJobStore;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The generated contract tests of GET /v1/validations/name-iban-match/{validationId}, against the
 * running service, with the job a case names put in the service's job store, or made sure is not.
 */
@DisplayName("The contract of GET /v1/validations/name-iban-match/{validationId}")
class GetNameIbanMatchJobContractTest extends RunningService implements GetNameIbanMatchJobContractTests {

    @Autowired
    private ValidationJobStore jobs;

    @Override
    public void arrangeState(ContractCase contractCase) {
        String validationId = contractCase.request().pathParameters().getFirst();
        switch (contractCase.id()) {
            case "success-200-required" -> jobs.claim(completedJob(validationId));
            case "not-found" -> assertEquals(Optional.empty(), jobs.find("name-iban-match", validationId),
                    "The job " + validationId + " must not exist");
            default -> throw new IllegalArgumentException("No state to arrange for case " + contractCase.id());
        }
    }

    /** A job that has run, as a validation submitted under an idempotency key of its own leaves it. */
    private static ValidationJob completedJob(String validationId) {
        NameIbanMatchResultV1 result = new NameIbanMatchResultV1(NameIbanMatchResultV1.ValidationEnum.NAME_IBAN_MATCH, OutcomeV1.VALID);
        return new ValidationJob(validationId, "name-iban-match", validationId, "request",
                "/payments/v1/validations/name-iban-match/" + validationId,
                ValidationJob.Status.COMPLETED, Optional.of(result), Optional.empty());
    }
}
