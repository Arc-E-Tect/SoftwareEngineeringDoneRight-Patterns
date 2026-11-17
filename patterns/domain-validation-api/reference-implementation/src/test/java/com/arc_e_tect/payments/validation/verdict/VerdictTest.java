package com.arc_e_tect.payments.validation.verdict;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A verdict")
class VerdictTest {

    private static final RuleCode RULE = new RuleCode("SOME_RULE");
    private static final Finding ERROR = Finding.of(RULE, Severity.ERROR, "Broken.");
    private static final Finding WARNING = Finding.of(RULE, Severity.WARNING, "Careful.");
    private static final Finding INFO = Finding.of(RULE, Severity.INFO, "For your information.");

    @Test
    @DisplayName("that is valid has the outcome valid")
    void validHasOutcomeValid() {
        assertEquals(Outcome.VALID, Verdict.valid("details").outcome());
    }

    @Test
    @DisplayName("that is valid carries its details")
    void validCarriesDetails() {
        assertEquals(Optional.of("details"), Verdict.valid("details").details());
    }

    @Test
    @DisplayName("that is valid can carry warnings and information")
    void validCarriesWarningsAndInformation() {
        assertEquals(List.of(WARNING, INFO), Verdict.valid("details", WARNING, INFO).findings());
    }

    @Test
    @DisplayName("that is valid is refused with an error, which would make it invalid")
    void validIsRefusedWithAnError() {
        assertThrows(IllegalArgumentException.class, () -> Verdict.valid("details", WARNING, ERROR));
    }

    @Test
    @DisplayName("that is valid is refused without details")
    void validIsRefusedWithoutDetails() {
        assertThrows(NullPointerException.class, () -> Verdict.valid(null));
    }

    @Test
    @DisplayName("that is invalid has the outcome invalid")
    void invalidHasOutcomeInvalid() {
        assertEquals(Outcome.INVALID, Verdict.invalid("details", ERROR).outcome());
    }

    @Test
    @DisplayName("that is invalid carries its details")
    void invalidCarriesDetails() {
        assertEquals(Optional.of("details"), Verdict.invalid("details", ERROR).details());
    }

    @Test
    @DisplayName("that is invalid carries its findings in order")
    void invalidCarriesFindingsInOrder() {
        assertEquals(List.of(INFO, ERROR, WARNING), Verdict.invalid("details", INFO, ERROR, WARNING).findings());
    }

    @Test
    @DisplayName("that is invalid is refused without an error that says why")
    void invalidIsRefusedWithoutAnError() {
        assertThrows(IllegalArgumentException.class, () -> Verdict.invalid("details", WARNING, INFO));
    }

    @Test
    @DisplayName("that is indeterminate has the outcome indeterminate")
    void indeterminateHasOutcomeIndeterminate() {
        assertEquals(Outcome.INDETERMINATE, Verdict.indeterminate(WARNING).outcome());
    }

    @Test
    @DisplayName("that is indeterminate carries no details")
    void indeterminateCarriesNoDetails() {
        assertEquals(Optional.empty(), Verdict.indeterminate(WARNING).details());
    }

    @Test
    @DisplayName("that is indeterminate carries its findings")
    void indeterminateCarriesFindings() {
        assertEquals(List.of(WARNING), Verdict.indeterminate(WARNING).findings());
    }

    @Test
    @DisplayName("that is indeterminate is refused with an error, which would be a verdict after all")
    void indeterminateIsRefusedWithAnError() {
        assertThrows(IllegalArgumentException.class, () -> Verdict.indeterminate(ERROR));
    }

    @Test
    @DisplayName("that is indeterminate is refused with details, as no verdict was reached")
    void indeterminateIsRefusedWithDetails() {
        assertThrows(IllegalArgumentException.class,
                () -> new Verdict<>(Outcome.INDETERMINATE, Optional.of("details"), List.of(WARNING)));
    }

    @Test
    @DisplayName("is refused without an outcome")
    void isRefusedWithoutAnOutcome() {
        assertThrows(NullPointerException.class, () -> new Verdict<>(null, Optional.of("details"), List.of()));
    }

    @Test
    @DisplayName("is refused without a details object")
    void isRefusedWithoutADetailsObject() {
        assertThrows(NullPointerException.class, () -> new Verdict<String>(Outcome.VALID, null, List.of()));
    }

    @Test
    @DisplayName("is refused without findings")
    void isRefusedWithoutFindings() {
        assertThrows(NullPointerException.class, () -> new Verdict<>(Outcome.VALID, Optional.of("details"), null));
    }

    @Test
    @DisplayName("keeps its findings when the list it was made from changes")
    void keepsItsFindingsWhenTheSourceListChanges() {
        List<Finding> source = new ArrayList<>(List.of(WARNING));
        Verdict<String> verdict = new Verdict<>(Outcome.VALID, Optional.of("details"), source);
        source.add(INFO);

        assertEquals(List.of(WARNING), verdict.findings());
    }

    @Test
    @DisplayName("does not let its findings be changed")
    void doesNotLetItsFindingsBeChanged() {
        List<Finding> findings = Verdict.valid("details", WARNING).findings();

        assertThrows(UnsupportedOperationException.class, () -> findings.add(INFO));
    }
}
