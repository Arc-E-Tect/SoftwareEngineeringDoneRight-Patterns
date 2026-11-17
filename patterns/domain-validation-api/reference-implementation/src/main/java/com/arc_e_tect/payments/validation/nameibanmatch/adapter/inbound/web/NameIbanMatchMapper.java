package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.FindingV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchDetailsV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchRequestV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchResultV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchJobV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.OutcomeV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.ProblemDetailsV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.ValidationJobStatusV1;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import org.springframework.stereotype.Component;

/**
 * Translates between the contract's types, generated into this adapter, and the use case's command
 * and result, so that neither the port nor the domain depends on generated code; and a validation
 * job of the asynchronous execution into the contract's job.
 */
@Component
public class NameIbanMatchMapper {

    public ValidateNameIbanMatchCommand toCommand(NameIbanMatchRequestV1 request) {
        return new ValidateNameIbanMatchCommand(request.getIban(), request.getAccountHolderName());
    }

    public NameIbanMatchResultV1 toResult(ValidateNameIbanMatchResult.Checked checked) {
        NameIbanMatchResultV1 result = new NameIbanMatchResultV1(
                NameIbanMatchResultV1.ValidationEnum.NAME_IBAN_MATCH,
                OutcomeV1.valueOf(checked.outcome().name()));
        result.setFindings(checked.findings().stream().map(NameIbanMatchMapper::finding).toList());
        checked.details().map(NameIbanMatchMapper::details).ifPresent(result::setDetails);
        return result;
    }

    /** The job's result is what this controller's validation answered: the contract's result. */
    public NameIbanMatchJobV1 toJob(ValidationJobView job) {
        NameIbanMatchJobV1 contract = new NameIbanMatchJobV1(job.validationId(), NameIbanMatchJobV1.ValidationEnum.NAME_IBAN_MATCH,
                ValidationJobStatusV1.valueOf(job.status().name()));
        job.result().map(NameIbanMatchResultV1.class::cast).ifPresent(contract::setResult);
        job.problem().map(NameIbanMatchMapper::problem).ifPresent(contract::setProblem);
        return contract;
    }

    private static ProblemDetailsV1 problem(ValidationJobView.Problem problem) {
        return new ProblemDetailsV1().type(problem.type()).title(problem.title()).status(problem.status()).detail(problem.detail());
    }

    private static NameIbanMatchDetailsV1 details(ValidateNameIbanMatchResult.Details details) {
        NameIbanMatchDetailsV1 contract = new NameIbanMatchDetailsV1(
                NameIbanMatchDetailsV1.MatchEnum.valueOf(details.match().name()));
        details.registeredName().ifPresent(contract::setRegisteredName);
        return contract;
    }

    private static FindingV1 finding(ValidateNameIbanMatchResult.Finding finding) {
        FindingV1 contract = new FindingV1(finding.rule(), FindingV1.SeverityEnum.valueOf(finding.severity().name()));
        contract.setMessage(finding.message());
        finding.pointer().ifPresent(contract::setPointer);
        return contract;
    }
}
