package com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound;

import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command.ValidateNameIbanMatchCommand;
import com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.result.ValidateNameIbanMatchResult;

/** Validates that the name a payer gives matches the name the account with the given IBAN is held by. */
public interface ValidateNameIbanMatchUseCase {

    ValidateNameIbanMatchResult validate(ValidateNameIbanMatchCommand command);
}
