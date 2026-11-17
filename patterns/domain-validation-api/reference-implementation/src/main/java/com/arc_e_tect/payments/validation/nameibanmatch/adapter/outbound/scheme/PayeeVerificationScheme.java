package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

import java.util.Optional;

/**
 * The payee verification scheme, as its client sees it: the name an account is registered to, by IBAN.
 * It answers no name for an account it does not know, and fails when it cannot answer.
 */
public interface PayeeVerificationScheme {

    Optional<String> registeredNameOf(String iban);
}
