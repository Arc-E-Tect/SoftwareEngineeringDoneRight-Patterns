package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import com.arc_e_tect.payments.validation.verdict.Finding;
import com.arc_e_tect.payments.validation.verdict.Outcome;
import com.arc_e_tect.payments.validation.verdict.RuleCode;
import com.arc_e_tect.payments.validation.verdict.Severity;
import com.arc_e_tect.payments.validation.verdict.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The name-match rule")
class NameMatchRuleTest {

    private static final Iban IBAN = new Iban("NL91ABNA0417164300");
    private static final RegisteredAccountHolder JAN = new RegisteredAccountHolder.Registered("Jan de Vries");
    private static final Finding MISMATCH = Finding.of(
                    new RuleCode("NAME_IBAN_MISMATCH"), Severity.ERROR,
                    "The name does not match the name of the account holder.")
            .at("/accountHolderName");
    private static final Finding CLOSE_MISMATCH = Finding.of(
                    new RuleCode("NAME_IBAN_MISMATCH"), Severity.ERROR,
                    "The name is close to, but not the same as, the name of the account holder.")
            .at("/accountHolderName");
    private static final Finding UNAVAILABLE = Finding.of(
            new RuleCode("PAYEE_VERIFICATION_UNAVAILABLE"), Severity.WARNING,
            "The account holder could not be verified at the moment.");

    private final NameMatchRule rule = new NameMatchRule();

    private Verdict<NameIbanMatchDetails> check(String name, RegisteredAccountHolder holder) {
        return rule.check(new PayeeClaim(IBAN, name), holder);
    }

    @Nested
    @DisplayName("for a name equal to the registered one, apart from case, whitespace and diacritics")
    class ExactMatch {

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"Jan de Vries", "JAN DE VRIES", " jan  de vries ", "Ján de Vríes", "Jan de Vries"})
        @DisplayName("is valid")
        void isValid(String name) {
            assertEquals(Outcome.VALID, check(name, JAN).outcome());
        }

        @Test
        @DisplayName("is an exact match")
        void isAnExactMatch() {
            assertEquals(Optional.of(NameIbanMatchDetails.exact()), check("JAN DE VRIES", JAN).details());
        }

        @Test
        @DisplayName("has no findings")
        void hasNoFindings() {
            assertEquals(List.of(), check("JAN DE VRIES", JAN).findings());
        }
    }

    @Nested
    @DisplayName("for a name similar to the registered one, at a similarity of 0.90 or more")
    class CloseMatch {

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"Jan de Vris", "Jan de Vreis", "Jan de Fries"})
        @DisplayName("is invalid")
        void isInvalid(String name) {
            assertEquals(Outcome.INVALID, check(name, JAN).outcome());
        }

        @Test
        @DisplayName("is a close match that names the registered name")
        void isACloseMatchWithTheRegisteredName() {
            assertEquals(Optional.of(NameIbanMatchDetails.close("Jan de Vries")), check("Jan de Vris", JAN).details());
        }

        @Test
        @DisplayName("finds that the name is close to the account holder's")
        void findsACloseMismatch() {
            assertEquals(List.of(CLOSE_MISMATCH), check("Jan de Vris", JAN).findings());
        }

        @Test
        @DisplayName("includes a similarity of exactly 0.90")
        void includesTheThreshold() {
            // abcd and abcdxyzq: 4 matching characters in order, so Jaro is (1 + 0.5 + 1) / 3 = 5/6,
            // and with a common prefix of 4, Jaro-Winkler is 5/6 + 4 * 0.1 * 1/6 = 0.9 exactly.
            RegisteredAccountHolder registered = new RegisteredAccountHolder.Registered("abcd");

            assertEquals(Outcome.INVALID, rule.check(new PayeeClaim(IBAN, "abcdxyzq"), registered).outcome());
        }

        @Test
        @DisplayName("at a similarity of exactly 0.90 is still a close match")
        void theThresholdIsACloseMatch() {
            RegisteredAccountHolder registered = new RegisteredAccountHolder.Registered("abcd");

            assertEquals(Optional.of(NameIbanMatchDetails.close("abcd")),
                    rule.check(new PayeeClaim(IBAN, "abcdxyzq"), registered).details());
        }
    }

    @Nested
    @DisplayName("for a name below a similarity of 0.90")
    class NoMatch {

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"Piet Jansen", "Anna Bakker", "Jansen"})
        @DisplayName("is invalid")
        void isInvalid(String name) {
            assertEquals(Outcome.INVALID, check(name, JAN).outcome());
        }

        @Test
        @DisplayName("is no match, and does not disclose the registered name")
        void isNoMatch() {
            assertEquals(Optional.of(NameIbanMatchDetails.none()), check("Piet Jansen", JAN).details());
        }

        @Test
        @DisplayName("finds that the name does not match")
        void findsAMismatch() {
            assertEquals(List.of(MISMATCH), check("Piet Jansen", JAN).findings());
        }

        @Test
        @DisplayName("is no match just below the threshold")
        void isNoMatchJustBelowTheThreshold() {
            // abcdef and abcdxy: Jaro 0.7778, a common prefix of 4, Jaro-Winkler 0.8667.
            RegisteredAccountHolder registered = new RegisteredAccountHolder.Registered("abcdef");

            assertEquals(Optional.of(NameIbanMatchDetails.none()),
                    rule.check(new PayeeClaim(IBAN, "abcdxy"), registered).details());
        }
    }

    @Nested
    @DisplayName("for an IBAN that no account holder is registered for")
    class UnknownAccount {

        @Test
        @DisplayName("is invalid")
        void isInvalid() {
            assertEquals(Outcome.INVALID, check("Jan de Vries", new RegisteredAccountHolder.Unknown()).outcome());
        }

        @Test
        @DisplayName("is no match")
        void isNoMatch() {
            assertEquals(Optional.of(NameIbanMatchDetails.none()), check("Jan de Vries", new RegisteredAccountHolder.Unknown()).details());
        }

        @Test
        @DisplayName("finds that the name does not match")
        void findsAMismatch() {
            assertEquals(List.of(MISMATCH), check("Jan de Vries", new RegisteredAccountHolder.Unknown()).findings());
        }
    }

    @Nested
    @DisplayName("when the account holder cannot be verified")
    class Unavailable {

        @Test
        @DisplayName("is indeterminate")
        void isIndeterminate() {
            assertEquals(Outcome.INDETERMINATE, check("Jan de Vries", new RegisteredAccountHolder.Unavailable()).outcome());
        }

        @Test
        @DisplayName("has no details")
        void hasNoDetails() {
            assertEquals(Optional.empty(), check("Jan de Vries", new RegisteredAccountHolder.Unavailable()).details());
        }

        @Test
        @DisplayName("warns that verification is unavailable")
        void warnsThatVerificationIsUnavailable() {
            assertEquals(List.of(UNAVAILABLE), check("Jan de Vries", new RegisteredAccountHolder.Unavailable()).findings());
        }
    }
}
