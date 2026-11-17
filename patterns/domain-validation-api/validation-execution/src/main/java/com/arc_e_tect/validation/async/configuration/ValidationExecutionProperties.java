package com.arc_e_tect.validation.async.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * The settings of the asynchronous execution: how long a client waits before it reads a pending
 * job, in whole seconds, the base of the problem types the execution answers with, and to whom the
 * completion of a job is published.
 */
@ConfigurationProperties("validation.execution")
public record ValidationExecutionProperties(@DefaultValue("1s") Duration retryAfter,
                                            @DefaultValue("https://api.example.com/problems/") URI problemTypes,
                                            @DefaultValue Events events) {

    public ValidationExecutionProperties {
        Objects.requireNonNull(retryAfter, "retryAfter");
        Objects.requireNonNull(problemTypes, "problemTypes");
        Objects.requireNonNull(events, "events");
        if (retryAfter.compareTo(Duration.ofSeconds(1)) < 0 || retryAfter.toMillisPart() != 0) {
            throw new IllegalArgumentException("Retry-After is a whole number of seconds, at least one: " + retryAfter);
        }
        if (!problemTypes.getPath().endsWith("/")) {
            throw new IllegalArgumentException("The base of the problem types ends in a slash: " + problemTypes);
        }
    }

    /** Where the completion of a job is published: to the webhook's subscribers, and to Kafka. */
    public record Events(@DefaultValue Webhook webhook, @DefaultValue Kafka kafka) {

        public Events {
            Objects.requireNonNull(webhook, "webhook");
            Objects.requireNonNull(kafka, "kafka");
        }
    }

    /** The subscribers of the webhook, none unless configured, and how long one may take to answer. */
    public record Webhook(@DefaultValue List<URI> subscribers, @DefaultValue("5s") Duration timeout) {

        public Webhook {
            subscribers = List.copyOf(Objects.requireNonNull(subscribers, "subscribers"));
            Objects.requireNonNull(timeout, "timeout");
            if (!timeout.isPositive()) {
                throw new IllegalArgumentException("A subscriber's timeout is positive: " + timeout);
            }
        }
    }

    /** Whether the completion is published to Kafka, off unless configured, and to which topic. */
    public record Kafka(boolean enabled, String topic) {

        public Kafka {
            if (enabled && (topic == null || topic.isBlank())) {
                throw new IllegalArgumentException("Publishing to Kafka needs a topic");
            }
        }
    }
}
