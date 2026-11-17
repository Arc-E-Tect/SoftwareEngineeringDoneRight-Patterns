package com.arc_e_tect.payments.validation.nameibanmatch.adapter.outbound.scheme;

/** The payee verification scheme cannot answer for an account, for instance because the other bank does not. */
public class PayeeVerificationSchemeUnavailableException extends RuntimeException {

    public PayeeVerificationSchemeUnavailableException(String iban) {
        super("The payee verification scheme cannot answer for " + iban);
    }
}
