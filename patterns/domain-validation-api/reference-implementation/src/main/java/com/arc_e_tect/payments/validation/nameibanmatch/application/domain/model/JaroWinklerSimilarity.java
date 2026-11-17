package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

/**
 * The Jaro-Winkler similarity of two strings, from 0 (nothing in common) to 1 (equal).
 * The standard definition: characters match within half the longer length minus one, and a
 * common prefix of at most four characters raises the similarity with a scale of 0.1.
 */
class JaroWinklerSimilarity {

    private static final int MAXIMUM_PREFIX = 4;
    private static final double PREFIX_SCALE = 0.1;

    double of(String first, String second) {
        if (first.equals(second)) {
            return 1.0;
        }
        double jaro = jaro(first, second);
        return jaro + commonPrefix(first, second) * PREFIX_SCALE * (1.0 - jaro);
    }

    private static double jaro(String first, String second) {
        int window = Math.max(0, Math.max(first.length(), second.length()) / 2 - 1);
        boolean[] firstMatched = new boolean[first.length()];
        boolean[] secondMatched = new boolean[second.length()];
        int matches = 0;
        for (int i = 0; i < first.length(); i++) {
            int end = Math.min(i + window + 1, second.length());
            for (int j = Math.max(0, i - window); j < end; j++) {
                if (!secondMatched[j] && first.charAt(i) == second.charAt(j)) {
                    firstMatched[i] = true;
                    secondMatched[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) {
            return 0.0;
        }
        int outOfOrder = 0;
        int j = 0;
        for (int i = 0; i < first.length(); i++) {
            if (firstMatched[i]) {
                while (!secondMatched[j]) {
                    j++;
                }
                if (first.charAt(i) != second.charAt(j)) {
                    outOfOrder++;
                }
                j++;
            }
        }
        double m = matches;
        return (m / first.length() + m / second.length() + (m - outOfOrder / 2.0) / m) / 3.0;
    }

    private static int commonPrefix(String first, String second) {
        int limit = Math.min(MAXIMUM_PREFIX, Math.min(first.length(), second.length()));
        int prefix = 0;
        while (prefix < limit && first.charAt(prefix) == second.charAt(prefix)) {
            prefix++;
        }
        return prefix;
    }
}
