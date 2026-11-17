package com.arc_e_tect.payments.validation.verdict;

/** How much a finding matters. Only an {@link #ERROR} makes a verdict invalid. */
public enum Severity {
    ERROR,
    WARNING,
    INFO
}
