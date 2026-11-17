package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.service;

import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.Iban;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.NameIbanMatchDetails;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.NameMatchRule;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.PayeeClaim;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.SupportedCountries;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.ValidateNameIbanMatchUseCase;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.outbound.AccountHolderRegistry;
import com.arc_e_tect.payments.validation.verdict.Finding;
import com.arc_e_tect.payments.validation.verdict.Verdict;

import java.util.Objects;

/**
 * Validates a name against an IBAN.
 *
 * <p>It refuses an IBAN whose checksum fails, and then one of a country the payee verification
 * scheme does not support, before it asks the register anything. The checksum comes first,
 * because a mistyped IBAN is the more likely cause, and that is what the payer can correct.
 * Otherwise it turns the command into the payer's claim, lets the rule check it against what the
 * register says, and turns the rule's verdict into the port's result.
 */
public class ValidateNameIbanMatchService implements ValidateNameIbanMatchUseCase {

    private final AccountHolderRegistry registry;
    private final NameMatchRule rule;
    private final SupportedCountries supportedCountries;

    public ValidateNameIbanMatchService(AccountHolderRegistry registry, NameMatchRule rule,
                                        SupportedCountries supportedCountries) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.rule = Objects.requireNonNull(rule, "rule");
        this.supportedCountries = Objects.requireNonNull(supportedCountries, "supportedCountries");
    }

    @Override
    public ValidateNameIbanMatchResult validate(ValidateNameIbanMatchCommand command) {
        Iban iban = new Iban(command.iban());
        if (!iban.hasValidChecksum()) {
            return new ValidateNameIbanMatchResult.InvalidChecksum(iban.value());
        }
        if (!supportedCountries.covers(iban)) {
            return new ValidateNameIbanMatchResult.UnsupportedCountry(iban.countryCode());
        }
        PayeeClaim claim = new PayeeClaim(iban, command.accountHolderName());
        return checked(rule.check(claim, registry.accountHolderOf(iban)));
    }

    private static ValidateNameIbanMatchResult.Checked checked(Verdict<NameIbanMatchDetails> verdict) {
        return new ValidateNameIbanMatchResult.Checked(
                ValidateNameIbanMatchResult.Outcome.valueOf(verdict.outcome().name()),
                verdict.details().map(ValidateNameIbanMatchService::details),
                verdict.findings().stream().map(ValidateNameIbanMatchService::finding).toList());
    }

    private static ValidateNameIbanMatchResult.Details details(NameIbanMatchDetails details) {
        return new ValidateNameIbanMatchResult.Details(
                ValidateNameIbanMatchResult.Match.valueOf(details.match().name()),
                details.registeredName());
    }

    private static ValidateNameIbanMatchResult.Finding finding(Finding finding) {
        return new ValidateNameIbanMatchResult.Finding(
                finding.rule().value(),
                ValidateNameIbanMatchResult.Severity.valueOf(finding.severity().name()),
                finding.message(),
                finding.pointer());
    }
}
