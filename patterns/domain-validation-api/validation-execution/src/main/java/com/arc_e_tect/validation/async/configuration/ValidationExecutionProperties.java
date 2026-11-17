package com.arc_e_tect.validation.async.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * The settings of the asynchronous execution: how long a client waits before it reads a pending
 * job, in whole seconds, and the base of the problem types the execution answers with.
 */
@ConfigurationProperties("validation.execution")
public record ValidationExecutionProperties(@DefaultValue("1s") Duration retryAfter,
                                            @DefaultValue("https://api.example.com/problems/") URI problemTypes) {

    public ValidationExecutionProperties {
        Objects.requireNonNull(retryAfter, "retryAfter");
        Objects.requireNonNull(problemTypes, "problemTypes");
        if (retryAfter.compareTo(Duration.ofSeconds(1)) < 0 || retryAfter.toMillisPart() != 0) {
            throw new IllegalArgumentException("Retry-After is a whole number of seconds, at least one: " + retryAfter);
        }
        if (!problemTypes.getPath().endsWith("/")) {
            throw new IllegalArgumentException("The base of the problem types ends in a slash: " + problemTypes);
        }
    }
}
