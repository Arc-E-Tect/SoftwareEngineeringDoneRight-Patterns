package com.arc_e_tect.validation.async.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The settings of the asynchronous execution")
class ValidationExecutionPropertiesTest {

    private static final URI PROBLEMS = URI.create("https://api.example.com/problems/");

    @Test
    @DisplayName("accept a Retry-After of one second")
    void acceptOneSecond() {
        assertEquals(Duration.ofSeconds(1), new ValidationExecutionProperties(Duration.ofSeconds(1), PROBLEMS).retryAfter());
    }

    @ParameterizedTest(name = "{0} ms")
    @ValueSource(longs = {0, 999, 1500})
    @DisplayName("are refused with a Retry-After that is not a whole number of seconds of at least one")
    void areRefusedWithAnotherRetryAfter(long millis) {
        Duration retryAfter = Duration.ofMillis(millis);

        assertThrows(IllegalArgumentException.class, () -> new ValidationExecutionProperties(retryAfter, PROBLEMS));
    }

    @Test
    @DisplayName("are refused with a base for problem types that does not end in a slash")
    void areRefusedWithABaseWithoutASlash() {
        URI base = URI.create("https://api.example.com/problems");

        assertThrows(IllegalArgumentException.class, () -> new ValidationExecutionProperties(Duration.ofSeconds(1), base));
    }

    @Test
    @DisplayName("are refused without a Retry-After")
    void areRefusedWithoutARetryAfter() {
        assertThrows(NullPointerException.class, () -> new ValidationExecutionProperties(null, PROBLEMS));
    }

    @Test
    @DisplayName("are refused without a base for problem types")
    void areRefusedWithoutABase() {
        assertThrows(NullPointerException.class, () -> new ValidationExecutionProperties(Duration.ofSeconds(1), null));
    }
}
