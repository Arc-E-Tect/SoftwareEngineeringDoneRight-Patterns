package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Makes two ways of writing the same name equal: no case, no diacritics, single spaces.
 *
 * <p>Whitespace is Unicode's: an em space or a no-break space is a space like any other. Nothing
 * more is folded: full-width letters and ligatures stay what they are, and no letter is
 * transliterated, so {@code ß} is not {@code ss} and {@code Ø} is not {@code O}.
 */
class NameNormaliser {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    String normalise(String name) {
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
        // Collapsed first, then stripped: strip() alone leaves a no-break space, which is not Java whitespace.
        return WHITESPACE.matcher(withoutDiacritics).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
    }
}
