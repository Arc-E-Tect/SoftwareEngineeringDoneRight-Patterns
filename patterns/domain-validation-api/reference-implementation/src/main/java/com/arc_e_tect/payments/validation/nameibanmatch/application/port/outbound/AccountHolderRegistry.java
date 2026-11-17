package com.arc_e_tect.payments.validation.nameibanmatch.application.port.outbound;

import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.Iban;
import com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model.RegisteredAccountHolder;

/**
 * The register of account holders: who holds the account with an IBAN.
 * When it cannot answer in time, it says so with {@link RegisteredAccountHolder.Unavailable}
 * instead of failing, so that the use case decides what that means.
 */
public interface AccountHolderRegistry {

    RegisteredAccountHolder accountHolderOf(Iban iban);
}
