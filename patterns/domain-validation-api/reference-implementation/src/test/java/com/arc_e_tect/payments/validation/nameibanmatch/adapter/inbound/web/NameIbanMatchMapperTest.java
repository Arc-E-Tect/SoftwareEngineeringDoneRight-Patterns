package com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.FindingV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchDetailsV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchRequestV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.NameIbanMatchResultV1;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.inbound.web.api.OutcomeV1;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Checked;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Details;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Finding;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Match;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Outcome;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("The name-iban-match mapper")
class NameIbanMatchMapperTest {

    private static final String MESSAGE = "The name is close to, but not the same as, the name of the account holder.";

    private final NameIbanMatchMapper mapper = new NameIbanMatchMapper();

    private static Checked checked(Outcome outcome, Optional<Details> details, Finding... findings) {
        return new Checked(outcome, details, List.of(findings));
    }

    @Test
    @DisplayName("turns the request into the use case's command")
    void turnsTheRequestIntoACommand() {
        assertEquals(new ValidateNameIbanMatchCommand("NL91ABNA0417164300", "Jan de Vries"),
                mapper.toCommand(new NameIbanMatchRequestV1("NL91ABNA0417164300", "Jan de Vries")));
    }

    @Nested
    @DisplayName("turns a checked result into the contract's result")
    class TurnsACheckedResultIntoTheContractsResult {

        @Test
        @DisplayName("of the name-iban-match validation")
        void ofTheValidation() {
            assertEquals(NameIbanMatchResultV1.ValidationEnum.NAME_IBAN_MATCH,
                    mapper.toResult(checked(Outcome.VALID, Optional.empty())).getValidation());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Outcome.class)
        @DisplayName("with the same outcome")
        void withTheSameOutcome(Outcome outcome) {
            assertEquals(outcome.name(), mapper.toResult(checked(outcome, Optional.empty())).getOutcome().name());
        }

        @Test
        @DisplayName("with the contract's value of the outcome")
        void withTheContractsValueOfTheOutcome() {
            assertEquals(OutcomeV1.INDETERMINATE, mapper.toResult(checked(Outcome.INDETERMINATE, Optional.empty())).getOutcome());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Match.class)
        @DisplayName("with the same match")
        void withTheSameMatch(Match match) {
            Optional<Details> details = Optional.of(new Details(match, Optional.empty()));

            assertEquals(match.name(), mapper.toResult(checked(Outcome.VALID, details)).getDetails().getMatch().name());
        }

        @Test
        @DisplayName("with the registered name of a close match")
        void withTheRegisteredName() {
            Optional<Details> details = Optional.of(new Details(Match.CLOSE, Optional.of("Jan de Vries")));

            assertEquals(new NameIbanMatchDetailsV1(NameIbanMatchDetailsV1.MatchEnum.CLOSE).registeredName("Jan de Vries"),
                    mapper.toResult(checked(Outcome.VALID, details)).getDetails());
        }

        @Test
        @DisplayName("without a registered name for any other match")
        void withoutARegisteredName() {
            Optional<Details> details = Optional.of(new Details(Match.EXACT, Optional.empty()));

            assertNull(mapper.toResult(checked(Outcome.VALID, details)).getDetails().getRegisteredName());
        }

        @Test
        @DisplayName("without details when no verdict was reached")
        void withoutDetails() {
            assertNull(mapper.toResult(checked(Outcome.INDETERMINATE, Optional.empty())).getDetails());
        }

        @Test
        @DisplayName("with a finding about a part of the input")
        void withAFindingAboutAPartOfTheInput() {
            Finding finding = new Finding("NAME_IBAN_MISMATCH", Severity.ERROR, MESSAGE, Optional.of("/accountHolderName"));

            assertEquals(List.of(new FindingV1("NAME_IBAN_MISMATCH", FindingV1.SeverityEnum.ERROR)
                            .message(MESSAGE).pointer("/accountHolderName")),
                    mapper.toResult(checked(Outcome.INVALID, Optional.empty(), finding)).getFindings());
        }

        @Test
        @DisplayName("with a finding about the input as a whole")
        void withAFindingAboutTheWholeInput() {
            Finding finding = new Finding("PAYEE_VERIFICATION_UNAVAILABLE", Severity.WARNING, MESSAGE, Optional.empty());

            assertEquals(List.of(new FindingV1("PAYEE_VERIFICATION_UNAVAILABLE", FindingV1.SeverityEnum.WARNING).message(MESSAGE)),
                    mapper.toResult(checked(Outcome.INDETERMINATE, Optional.empty(), finding)).getFindings());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Severity.class)
        @DisplayName("with findings of the same severity")
        void withFindingsOfTheSameSeverity(Severity severity) {
            Finding finding = new Finding("RULE", severity, MESSAGE, Optional.empty());

            assertEquals(severity.name(),
                    mapper.toResult(checked(Outcome.VALID, Optional.empty(), finding)).getFindings().getFirst().getSeverity().name());
        }
    }
}
