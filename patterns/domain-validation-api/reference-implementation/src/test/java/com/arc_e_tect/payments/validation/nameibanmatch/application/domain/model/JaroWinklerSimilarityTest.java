package com.arc_e_tect.payments.validation.nameibanmatch.application.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The Jaro-Winkler similarity")
class JaroWinklerSimilarityTest {

    private static final double PRECISION = 0.0001;

    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    @ParameterizedTest(name = "of {0} and {1} is {2}")
    @CsvSource({
            // MARTHA, DWAYNE and DIXON are the textbook examples; the others follow from the standard
            // definition: a matching window of half the longer length minus one, a prefix of at most 4,
            // and a prefix scale of 0.1.
            "MARTHA, MARHTA, 0.9611",
            "DWAYNE, DUANE, 0.8400",
            "DIXON, DICKSONX, 0.8133",
            "JELLYFISH, SMELLYFISH, 0.8963",
            "CRATE, TRACE, 0.7333",
            "ABCD, ABCE, 0.8833",
            "AB, BA, 0.0000",
            "A, A, 1.0000",
            "A, B, 0.0000",
            "ABC, XYZ, 0.0000",
            "ABCDEF, ABCDEX, 0.9333"
    })
    @DisplayName("matches the reference values")
    void matchesReferenceValues(String first, String second, double expected) {
        assertEquals(expected, similarity.of(first, second), PRECISION);
    }

    @ParameterizedTest(name = "of {0} and {1}")
    @CsvSource({"MARTHA, MARHTA", "DWAYNE, DUANE", "DIXON, DICKSONX"})
    @DisplayName("is the same in both directions")
    void isSymmetric(String first, String second) {
        assertEquals(similarity.of(first, second), similarity.of(second, first), PRECISION);
    }

    @Test
    @DisplayName("of two empty names is 1, as they are equal")
    void ofTwoEmptyNamesIsOne() {
        assertEquals(1.0, similarity.of("", ""), PRECISION);
    }

    @Test
    @DisplayName("of an empty name and another is 0")
    void ofAnEmptyNameAndAnotherIsZero() {
        assertEquals(0.0, similarity.of("", "JAN"), PRECISION);
    }

    @Test
    @DisplayName("of a name and an empty one is 0")
    void ofANameAndAnEmptyOneIsZero() {
        assertEquals(0.0, similarity.of("JAN", ""), PRECISION);
    }
}
