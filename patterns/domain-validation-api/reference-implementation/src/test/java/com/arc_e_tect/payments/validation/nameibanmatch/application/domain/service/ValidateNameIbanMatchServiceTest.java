package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.service;

import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.Iban;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.NameIbanMatchDetails;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.NameMatchRule;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.PayeeClaim;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.RegisteredAccountHolder;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.SupportedCountries;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.outbound.AccountHolderRegistry;
import com.arc_e_tect.payments.validation.verdict.Finding;
import com.arc_e_tect.payments.validation.verdict.RuleCode;
import com.arc_e_tect.payments.validation.verdict.Severity;
import com.arc_e_tect.payments.validation.verdict.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The service that validates a name against an IBAN")
class ValidateNameIbanMatchServiceTest {

    private static final String NAME = "Jan de Vries";
    private static final String DUTCH_IBAN = "NL91ABNA0417164300";
    private static final String DUTCH_IBAN_WITH_A_FAILING_CHECKSUM = "NL91ABNA0417164301";
    private static final String BELGIAN_IBAN = "BE68539007547034";
    private static final String BRITISH_IBAN_WITH_A_FAILING_CHECKSUM = "GB82WEST12345698765433";
    private static final RegisteredAccountHolder JAN = new RegisteredAccountHolder.Registered(NAME);
    private static final RuleCode RULE = new RuleCode("NAME_IBAN_MISMATCH");
    private static final String MESSAGE = "The name does not match the name of the account holder.";

    private final List<Iban> askedRegister = new ArrayList<>();
    private final AccountHolderRegistry registry = iban -> {
        askedRegister.add(iban);
        return JAN;
    };
    private final StubRule rule = new StubRule();
    private final ValidateNameIbanMatchService service =
            new ValidateNameIbanMatchService(registry, rule, SupportedCountries.of("NL", "DE"));

    private ValidateNameIbanMatchResult validate(String iban) {
        return service.validate(new ValidateNameIbanMatchCommand(iban, NAME));
    }

    private ValidateNameIbanMatchResult.Checked checked(Verdict<NameIbanMatchDetails> verdict) {
        rule.verdict = verdict;
        return (ValidateNameIbanMatchResult.Checked) validate(DUTCH_IBAN);
    }

    /** Answers with the verdict it is given, and remembers what it was asked. */
    private static final class StubRule extends NameMatchRule {

        private Verdict<NameIbanMatchDetails> verdict = Verdict.valid(NameIbanMatchDetails.exact());
        private PayeeClaim claim;
        private RegisteredAccountHolder holder;

        @Override
        public Verdict<NameIbanMatchDetails> check(PayeeClaim claim, RegisteredAccountHolder holder) {
            this.claim = claim;
            this.holder = holder;
            return verdict;
        }
    }

    @Nested
    @DisplayName("for an IBAN whose checksum fails")
    class ForAFailingChecksum {

        @Test
        @DisplayName("refuses it as an invalid checksum")
        void refusesItAsAnInvalidChecksum() {
            assertEquals(new ValidateNameIbanMatchResult.InvalidChecksum(DUTCH_IBAN_WITH_A_FAILING_CHECKSUM),
                    validate(DUTCH_IBAN_WITH_A_FAILING_CHECKSUM));
        }

        @Test
        @DisplayName("refuses it as an invalid checksum, even when its country is not supported either")
        void refusesItAsAnInvalidChecksumBeforeItsCountry() {
            assertEquals(new ValidateNameIbanMatchResult.InvalidChecksum(BRITISH_IBAN_WITH_A_FAILING_CHECKSUM),
                    validate(BRITISH_IBAN_WITH_A_FAILING_CHECKSUM));
        }

        @Test
        @DisplayName("does not ask the register")
        void doesNotAskTheRegister() {
            validate(DUTCH_IBAN_WITH_A_FAILING_CHECKSUM);

            assertEquals(List.of(), askedRegister);
        }
    }

    @Nested
    @DisplayName("for an IBAN of a country the scheme does not support")
    class ForAnUnsupportedCountry {

        @Test
        @DisplayName("refuses it as an unsupported country")
        void refusesItAsAnUnsupportedCountry() {
            assertEquals(new ValidateNameIbanMatchResult.UnsupportedCountry("BE"), validate(BELGIAN_IBAN));
        }

        @Test
        @DisplayName("does not ask the register")
        void doesNotAskTheRegister() {
            validate(BELGIAN_IBAN);

            assertEquals(List.of(), askedRegister);
        }
    }

    @Nested
    @DisplayName("for an IBAN it can check")
    class ForAnIbanItCanCheck {

        @Test
        @DisplayName("asks the register about the IBAN")
        void asksTheRegisterAboutTheIban() {
            validate(DUTCH_IBAN);

            assertEquals(List.of(new Iban(DUTCH_IBAN)), askedRegister);
        }

        @Test
        @DisplayName("lets the rule check the payer's claim")
        void letsTheRuleCheckTheClaim() {
            validate(DUTCH_IBAN);

            assertEquals(new PayeeClaim(new Iban(DUTCH_IBAN), NAME), rule.claim);
        }

        @Test
        @DisplayName("lets the rule check it against the register's answer")
        void letsTheRuleCheckAgainstTheRegistersAnswer() {
            validate(DUTCH_IBAN);

            assertEquals(JAN, rule.holder);
        }
    }

    @Nested
    @DisplayName("turns the rule's verdict into a checked result")
    class TurnsTheVerdictIntoACheckedResult {

        static Stream<Arguments> outcomes() {
            return Stream.of(
                    Arguments.of(Verdict.valid(NameIbanMatchDetails.exact()), ValidateNameIbanMatchResult.Outcome.VALID),
                    Arguments.of(Verdict.invalid(NameIbanMatchDetails.none(), Finding.of(RULE, Severity.ERROR, MESSAGE)),
                            ValidateNameIbanMatchResult.Outcome.INVALID),
                    Arguments.of(Verdict.indeterminate(), ValidateNameIbanMatchResult.Outcome.INDETERMINATE));
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("outcomes")
        @DisplayName("with the same outcome")
        void withTheSameOutcome(Verdict<NameIbanMatchDetails> verdict, ValidateNameIbanMatchResult.Outcome outcome) {
            assertEquals(outcome, checked(verdict).outcome());
        }

        static Stream<Arguments> details() {
            return Stream.of(
                    Arguments.of(NameIbanMatchDetails.exact(),
                            new ValidateNameIbanMatchResult.Details(ValidateNameIbanMatchResult.Match.EXACT, Optional.empty())),
                    Arguments.of(NameIbanMatchDetails.close(NAME),
                            new ValidateNameIbanMatchResult.Details(ValidateNameIbanMatchResult.Match.CLOSE, Optional.of(NAME))),
                    Arguments.of(NameIbanMatchDetails.none(),
                            new ValidateNameIbanMatchResult.Details(ValidateNameIbanMatchResult.Match.NONE, Optional.empty())));
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("details")
        @DisplayName("with the same match and registered name")
        void withTheSameDetails(NameIbanMatchDetails details, ValidateNameIbanMatchResult.Details expected) {
            assertEquals(Optional.of(expected), checked(Verdict.valid(details)).details());
        }

        @Test
        @DisplayName("without details when no verdict was reached")
        void withoutDetailsWhenIndeterminate() {
            assertEquals(Optional.empty(), checked(Verdict.indeterminate()).details());
        }

        @Test
        @DisplayName("with a finding about a part of the input")
        void withAFindingAboutAPartOfTheInput() {
            Finding finding = Finding.of(RULE, Severity.ERROR, MESSAGE).at("/accountHolderName");

            assertEquals(List.of(new ValidateNameIbanMatchResult.Finding("NAME_IBAN_MISMATCH",
                            ValidateNameIbanMatchResult.Severity.ERROR, MESSAGE, Optional.of("/accountHolderName"))),
                    checked(Verdict.invalid(NameIbanMatchDetails.none(), finding)).findings());
        }

        @Test
        @DisplayName("with a finding about the input as a whole")
        void withAFindingAboutTheWholeInput() {
            Finding finding = Finding.of(RULE, Severity.WARNING, MESSAGE);

            assertEquals(List.of(new ValidateNameIbanMatchResult.Finding("NAME_IBAN_MISMATCH",
                            ValidateNameIbanMatchResult.Severity.WARNING, MESSAGE, Optional.empty())),
                    checked(Verdict.indeterminate(finding)).findings());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Severity.class)
        @DisplayName("with findings of the same severity")
        void withFindingsOfTheSameSeverity(Severity severity) {
            Finding finding = Finding.of(RULE, severity, MESSAGE);
            Verdict<NameIbanMatchDetails> verdict = severity == Severity.ERROR
                    ? Verdict.invalid(NameIbanMatchDetails.none(), finding)
                    : Verdict.valid(NameIbanMatchDetails.exact(), finding);

            assertEquals(severity.name(), checked(verdict).findings().getFirst().severity().name());
        }
    }

    @Nested
    @DisplayName("is refused")
    class IsRefused {

        @Test
        @DisplayName("without a register of account holders")
        void withoutARegister() {
            SupportedCountries countries = SupportedCountries.of("NL");

            assertThrows(NullPointerException.class, () -> new ValidateNameIbanMatchService(null, rule, countries));
        }

        @Test
        @DisplayName("without a rule")
        void withoutARule() {
            SupportedCountries countries = SupportedCountries.of("NL");

            assertThrows(NullPointerException.class, () -> new ValidateNameIbanMatchService(registry, null, countries));
        }

        @Test
        @DisplayName("without the supported countries")
        void withoutTheSupportedCountries() {
            assertThrows(NullPointerException.class, () -> new ValidateNameIbanMatchService(registry, rule, null));
        }
    }
}
