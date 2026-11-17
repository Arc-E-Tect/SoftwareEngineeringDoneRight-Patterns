package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

/** How the name the payer gives compares with the registered name. */
public enum Match {
    /** Equal, apart from case, whitespace and diacritics. */
    EXACT,
    /** Similar enough to be a typing error. */
    CLOSE,
    /** Neither. */
    NONE
}
