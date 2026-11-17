package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Makes two ways of writing the same name equal: no case, no diacritics, single spaces. */
class NameNormaliser {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    String normalise(String name) {
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
        return WHITESPACE.matcher(withoutDiacritics.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }
}
