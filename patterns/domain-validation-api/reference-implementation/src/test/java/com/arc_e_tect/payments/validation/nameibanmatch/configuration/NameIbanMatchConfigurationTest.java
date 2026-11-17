package com.arc_e_tect.payments.validation.nameibanmatch.configuration;

import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.RegisteredAccountHolder;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.ValidateNameIbanMatchUseCase;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The configuration of the name-iban-match validation")
class NameIbanMatchConfigurationTest {

    private final ValidateNameIbanMatchUseCase useCase = new NameIbanMatchConfiguration().validateNameIbanMatchUseCase(
            iban -> new RegisteredAccountHolder.Registered("Jan de Vries"), new NameIbanMatchProperties(Set.of("NL")));

    @Test
    @DisplayName("validates a name against the register it is given")
    void validatesAgainstTheRegister() {
        ValidateNameIbanMatchResult.Checked checked = (ValidateNameIbanMatchResult.Checked)
                useCase.validate(new ValidateNameIbanMatchCommand("NL91ABNA0417164300", "Jan de Vries"));

        assertEquals(ValidateNameIbanMatchResult.Outcome.VALID, checked.outcome());
    }

    @Test
    @DisplayName("validates for the configured countries only")
    void validatesForTheConfiguredCountriesOnly() {
        assertEquals(new ValidateNameIbanMatchResult.UnsupportedCountry("BE"),
                useCase.validate(new ValidateNameIbanMatchCommand("BE68539007547034", "Jan de Vries")));
    }
}
