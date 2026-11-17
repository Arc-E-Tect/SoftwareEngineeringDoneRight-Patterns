package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import com.arc_e_tect.payments.validation.verdict.Finding;
import com.arc_e_tect.payments.validation.verdict.RuleCode;
import com.arc_e_tect.payments.validation.verdict.Severity;
import com.arc_e_tect.payments.validation.verdict.Verdict;

/**
 * Whether the name a payer gives matches the name the account is registered to.
 *
 * <p>Equal after normalising case, whitespace and diacritics is an exact match, and valid.
 * A Jaro-Winkler similarity of at least 0.90 is a close match: invalid, with the registered
 * name, so the payer can correct a typing error. Anything less, and an account nobody is
 * registered for, is no match: invalid. When the register cannot be reached, the verdict is
 * indeterminate: answering invalid would block a legitimate payment, and answering valid would
 * remove the protection against misdirected payments the validation exists for.
 */
public class NameMatchRule {

    public static final RuleCode NAME_IBAN_MISMATCH = new RuleCode("NAME_IBAN_MISMATCH");
    public static final RuleCode PAYEE_VERIFICATION_UNAVAILABLE = new RuleCode("PAYEE_VERIFICATION_UNAVAILABLE");

    static final double CLOSE_MATCH_THRESHOLD = 0.90;

    private static final String NAME = "/accountHolderName";
    private static final Finding MISMATCH = Finding.of(NAME_IBAN_MISMATCH, Severity.ERROR,
            "The name does not match the name of the account holder.").at(NAME);
    private static final Finding CLOSE_MISMATCH = Finding.of(NAME_IBAN_MISMATCH, Severity.ERROR,
            "The name is close to, but not the same as, the name of the account holder.").at(NAME);
    private static final Finding UNAVAILABLE = Finding.of(PAYEE_VERIFICATION_UNAVAILABLE, Severity.WARNING,
            "The account holder could not be verified at the moment.");

    private final NameNormaliser normaliser = new NameNormaliser();
    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public Verdict<NameIbanMatchDetails> check(PayeeClaim claim, RegisteredAccountHolder holder) {
        return switch (holder) {
            case RegisteredAccountHolder.Registered registered -> compare(claim.accountHolderName(), registered.name());
            case RegisteredAccountHolder.Unknown unknown -> Verdict.invalid(NameIbanMatchDetails.none(), MISMATCH);
            case RegisteredAccountHolder.Unavailable unavailable -> Verdict.indeterminate(UNAVAILABLE);
        };
    }

    private Verdict<NameIbanMatchDetails> compare(String given, String registered) {
        String normalisedGiven = normaliser.normalise(given);
        String normalisedRegistered = normaliser.normalise(registered);
        if (normalisedGiven.equals(normalisedRegistered)) {
            return Verdict.valid(NameIbanMatchDetails.exact());
        }
        if (similarity.of(normalisedGiven, normalisedRegistered) >= CLOSE_MATCH_THRESHOLD) {
            return Verdict.invalid(NameIbanMatchDetails.close(registered), CLOSE_MISMATCH);
        }
        return Verdict.invalid(NameIbanMatchDetails.none(), MISMATCH);
    }
}
