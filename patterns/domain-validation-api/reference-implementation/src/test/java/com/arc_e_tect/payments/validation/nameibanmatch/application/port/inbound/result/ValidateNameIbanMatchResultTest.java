package com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result;

import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Checked;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Details;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Finding;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.InvalidChecksum;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Match;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Outcome;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Severity;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.UnsupportedCountry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The result of validating a name against an IBAN")
class ValidateNameIbanMatchResultTest {

    private static final Finding MISMATCH = new Finding("NAME_IBAN_MISMATCH", Severity.ERROR,
            "The name does not match the name of the account holder.", Optional.of("/accountHolderName"));
    private static final Optional<Details> NONE = Optional.of(new Details(Match.NONE, Optional.empty()));

    @Nested
    @DisplayName("when the rule was checked")
    class WhenChecked {

        @Test
        @DisplayName("does not change when the list it was made from changes")
        void doesNotChangeWithItsSource() {
            List<Finding> findings = new ArrayList<>(List.of(MISMATCH));
            Checked checked = new Checked(Outcome.INVALID, NONE, findings);

            findings.clear();

            assertEquals(List.of(MISMATCH), checked.findings());
        }

        @Test
        @DisplayName("cannot have its findings changed")
        void hasUnmodifiableFindings() {
            Checked checked = new Checked(Outcome.INVALID, NONE, List.of(MISMATCH));

            assertThrows(UnsupportedOperationException.class, () -> checked.findings().add(MISMATCH));
        }

        @Test
        @DisplayName("is refused without an outcome")
        void isRefusedWithoutAnOutcome() {
            assertThrows(NullPointerException.class, () -> new Checked(null, NONE, List.of(MISMATCH)));
        }

        @Test
        @DisplayName("is refused without its optional details")
        void isRefusedWithoutDetails() {
            assertThrows(NullPointerException.class, () -> new Checked(Outcome.INVALID, null, List.of(MISMATCH)));
        }

        @Test
        @DisplayName("is refused without a list of findings")
        void isRefusedWithoutFindings() {
            assertThrows(NullPointerException.class, () -> new Checked(Outcome.INVALID, NONE, null));
        }
    }

    @Nested
    @DisplayName("has details that")
    class HasDetails {

        @Test
        @DisplayName("are refused without a match")
        void areRefusedWithoutAMatch() {
            assertThrows(NullPointerException.class, () -> new Details(null, Optional.empty()));
        }

        @Test
        @DisplayName("are refused without the optional registered name")
        void areRefusedWithoutARegisteredName() {
            assertThrows(NullPointerException.class, () -> new Details(Match.NONE, null));
        }
    }

    @Nested
    @DisplayName("has findings that")
    class HasFindings {

        @Test
        @DisplayName("are refused without a rule")
        void areRefusedWithoutARule() {
            assertThrows(NullPointerException.class,
                    () -> new Finding(null, Severity.ERROR, "message", Optional.empty()));
        }

        @Test
        @DisplayName("are refused without a severity")
        void areRefusedWithoutASeverity() {
            assertThrows(NullPointerException.class,
                    () -> new Finding("NAME_IBAN_MISMATCH", null, "message", Optional.empty()));
        }

        @Test
        @DisplayName("are refused without a message")
        void areRefusedWithoutAMessage() {
            assertThrows(NullPointerException.class,
                    () -> new Finding("NAME_IBAN_MISMATCH", Severity.ERROR, null, Optional.empty()));
        }

        @Test
        @DisplayName("are refused without the optional pointer")
        void areRefusedWithoutAPointer() {
            assertThrows(NullPointerException.class,
                    () -> new Finding("NAME_IBAN_MISMATCH", Severity.ERROR, "message", null));
        }
    }

    @Nested
    @DisplayName("when the request is refused")
    class WhenRefused {

        @Test
        @DisplayName("for its country, is refused without a country code")
        void unsupportedCountryIsRefusedWithoutACountryCode() {
            assertThrows(NullPointerException.class, () -> new UnsupportedCountry(null));
        }

        @Test
        @DisplayName("for its checksum, is refused without the IBAN")
        void invalidChecksumIsRefusedWithoutTheIban() {
            assertThrows(NullPointerException.class, () -> new InvalidChecksum(null));
        }
    }
}
