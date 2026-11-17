package com.arc_e_tect.payments.validation.verdict;

/**
 * The verdict of a validation.
 * Three-valued on purpose: a validation that cannot conclude is {@link #INDETERMINATE},
 * rather than forced to give a wrong answer.
 */
public enum Outcome {
    /** The input was checked and satisfies the rules. */
    VALID,
    /** The input was checked and violates the rules; the findings say which. */
    INVALID,
    /** The input was checked, but no verdict could be reached. */
    INDETERMINATE
}
