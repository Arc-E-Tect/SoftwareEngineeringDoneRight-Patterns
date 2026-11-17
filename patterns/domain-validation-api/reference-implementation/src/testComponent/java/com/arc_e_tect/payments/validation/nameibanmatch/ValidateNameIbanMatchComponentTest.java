package com.arc_e_tect.payments.validation.nameibanmatch;

import com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme.PayeeVerificationSchemeClient;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme.SimulatedAccount;
import com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme.SimulatedPayeeVerificationScheme;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.NameMatchRule;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.SupportedCountries;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.service.ValidateNameIbanMatchService;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.ValidateNameIbanMatchUseCase;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Checked;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Details;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Match;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult.Outcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The name-iban-match use case as one component: the domain service, the rule and the client of the
 * payee verification scheme, with the simulated scheme in place of the real one.
 */
@DisplayName("Validating a name against an IBAN, through the simulated payee verification scheme")
class ValidateNameIbanMatchComponentTest {

    private static final String JAN = "NL91ABNA0417164300";
    private static final String SLOW = "DE89370400440532013000";
    private static final String UNAVAILABLE = "NL20INGB0001234567";
    private static final String UNKNOWN = "NL39RABO0300065264";

    private final ValidateNameIbanMatchUseCase useCase = new ValidateNameIbanMatchService(
            new PayeeVerificationSchemeClient(
                    new SimulatedPayeeVerificationScheme(Map.of(
                            JAN, SimulatedAccount.of("Jan de Vries"),
                            SLOW, SimulatedAccount.of("Anna Schmidt").slow(Duration.ofSeconds(5)),
                            UNAVAILABLE, SimulatedAccount.of("Piet Jansen").unavailable())),
                    Duration.ofMillis(200)),
            new NameMatchRule(),
            SupportedCountries.of("NL", "DE"));

    private ValidateNameIbanMatchResult validate(String iban, String name) {
        return useCase.validate(new ValidateNameIbanMatchCommand(iban, name));
    }

    @Test
    @DisplayName("is valid with an exact match for the registered name")
    void exactMatch() {
        assertEquals(Optional.of(new Details(Match.EXACT, Optional.empty())),
                ((Checked) validate(JAN, "jan de vries")).details());
    }

    @Test
    @DisplayName("is invalid with a close match, naming the registered name, for a name close to it")
    void closeMatch() {
        assertEquals(Optional.of(new Details(Match.CLOSE, Optional.of("Jan de Vries"))),
                ((Checked) validate(JAN, "Jan de Vris")).details());
    }

    @Test
    @DisplayName("is invalid with no match for another name")
    void noMatch() {
        assertEquals(Optional.of(new Details(Match.NONE, Optional.empty())),
                ((Checked) validate(JAN, "Kees Bakker")).details());
    }

    @Test
    @DisplayName("is invalid with no match for an account the scheme does not know")
    void unknownAccount() {
        assertEquals(Optional.of(new Details(Match.NONE, Optional.empty())),
                ((Checked) validate(UNKNOWN, "Jan de Vries")).details());
    }

    @Test
    @DisplayName("is indeterminate when the scheme does not answer in time")
    void slowAccount() {
        assertEquals(Outcome.INDETERMINATE, ((Checked) validate(SLOW, "Anna Schmidt")).outcome());
    }

    @Test
    @DisplayName("is indeterminate when the scheme cannot answer")
    void unavailableAccount() {
        assertEquals(Outcome.INDETERMINATE, ((Checked) validate(UNAVAILABLE, "Piet Jansen")).outcome());
    }
}
