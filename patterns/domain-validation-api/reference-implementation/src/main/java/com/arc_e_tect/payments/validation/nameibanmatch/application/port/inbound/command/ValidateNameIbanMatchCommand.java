package com.arc_e_tect.payments.validation.nameibanmatch.application.port.inbound.command;

import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.Iban;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.PayeeClaim;

/**
 * The input of {@code ValidateNameIbanMatchUseCase}: an IBAN in electronic format and the name the
 * payer gives for its account holder.
 *
 * <p>The command refuses input that can never be valid, by the same checks as the domain: an IBAN
 * that is not in electronic format, and a name that is blank or longer than 70 characters. So
 * whichever adapter drives the use case, it gets a result, never an exception. An IBAN whose
 * checksum fails is well-formed; the use case refuses it with its own result.
 */
public record ValidateNameIbanMatchCommand(String iban, String accountHolderName) {

    public ValidateNameIbanMatchCommand {
        new PayeeClaim(new Iban(iban), accountHolderName);
    }
}
