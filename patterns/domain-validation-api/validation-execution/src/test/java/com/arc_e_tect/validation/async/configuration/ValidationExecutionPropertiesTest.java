package com.arc_e_tect.validation.async.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The settings of the asynchronous execution")
class ValidationExecutionPropertiesTest {

    private static final URI PROBLEMS = URI.create("https://api.example.com/problems/");
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final ValidationExecutionProperties.Events NONE = new ValidationExecutionProperties.Events(
            new ValidationExecutionProperties.Webhook(List.of(), TIMEOUT), new ValidationExecutionProperties.Kafka(false, null));

    @Test
    @DisplayName("accept a Retry-After of one second")
    void acceptOneSecond() {
        assertEquals(Duration.ofSeconds(1), new ValidationExecutionProperties(Duration.ofSeconds(1), PROBLEMS, NONE).retryAfter());
    }

    @ParameterizedTest(name = "{0} ms")
    @ValueSource(longs = {0, 999, 1500})
    @DisplayName("are refused with a Retry-After that is not a whole number of seconds of at least one")
    void areRefusedWithAnotherRetryAfter(long millis) {
        Duration retryAfter = Duration.ofMillis(millis);

        assertThrows(IllegalArgumentException.class, () -> new ValidationExecutionProperties(retryAfter, PROBLEMS, NONE));
    }

    @Test
    @DisplayName("are refused with a base for problem types that does not end in a slash")
    void areRefusedWithABaseWithoutASlash() {
        URI base = URI.create("https://api.example.com/problems");

        assertThrows(IllegalArgumentException.class, () -> new ValidationExecutionProperties(Duration.ofSeconds(1), base, NONE));
    }

    @Test
    @DisplayName("are refused without a Retry-After")
    void areRefusedWithoutARetryAfter() {
        assertThrows(NullPointerException.class, () -> new ValidationExecutionProperties(null, PROBLEMS, NONE));
    }

    @Test
    @DisplayName("are refused without a base for problem types")
    void areRefusedWithoutABase() {
        assertThrows(NullPointerException.class, () -> new ValidationExecutionProperties(Duration.ofSeconds(1), null, NONE));
    }

    @Test
    @DisplayName("are refused without the settings of the events")
    void areRefusedWithoutEvents() {
        Duration retryAfter = Duration.ofSeconds(1);

        assertThrows(NullPointerException.class, () -> new ValidationExecutionProperties(retryAfter, PROBLEMS, null));
    }

    @Test
    @DisplayName("of the events are refused without those of the webhook")
    void eventsAreRefusedWithoutTheWebhook() {
        ValidationExecutionProperties.Kafka kafka = NONE.kafka();

        assertThrows(NullPointerException.class, () -> new ValidationExecutionProperties.Events(null, kafka));
    }

    @Test
    @DisplayName("of the events are refused without those of Kafka")
    void eventsAreRefusedWithoutKafka() {
        ValidationExecutionProperties.Webhook webhook = NONE.webhook();

        assertThrows(NullPointerException.class, () -> new ValidationExecutionProperties.Events(webhook, null));
    }

    @Test
    @DisplayName("of the webhook keep their own copy of the subscribers")
    void webhookKeepsItsOwnSubscribers() {
        List<URI> subscribers = new ArrayList<>(List.of(URI.create("https://subscriber.example.org/hooks")));
        ValidationExecutionProperties.Webhook webhook = new ValidationExecutionProperties.Webhook(subscribers, TIMEOUT);
        subscribers.clear();

        assertEquals(List.of(URI.create("https://subscriber.example.org/hooks")), webhook.subscribers());
    }

    @Test
    @DisplayName("of the webhook are refused without subscribers")
    void webhookIsRefusedWithoutSubscribers() {
        assertThrows(NullPointerException.class, () -> new ValidationExecutionProperties.Webhook(null, TIMEOUT));
    }

    @Test
    @DisplayName("of the webhook are refused without a timeout")
    void webhookIsRefusedWithoutATimeout() {
        List<URI> subscribers = List.of();

        assertThrows(NullPointerException.class, () -> new ValidationExecutionProperties.Webhook(subscribers, null));
    }

    @ParameterizedTest(name = "{0} ms")
    @ValueSource(longs = {0, -1})
    @DisplayName("of the webhook are refused with a timeout that is not positive")
    void webhookIsRefusedWithATimeoutThatIsNotPositive(long millis) {
        List<URI> subscribers = List.of();
        Duration timeout = Duration.ofMillis(millis);

        assertThrows(IllegalArgumentException.class, () -> new ValidationExecutionProperties.Webhook(subscribers, timeout));
    }

    @Test
    @DisplayName("of the webhook accept a timeout of a millisecond")
    void webhookAcceptsAMillisecond() {
        assertEquals(Duration.ofMillis(1), new ValidationExecutionProperties.Webhook(List.of(), Duration.ofMillis(1)).timeout());
    }

    @Test
    @DisplayName("of Kafka accept no topic while publication is off")
    void kafkaAcceptsNoTopicWhileOff() {
        assertDoesNotThrow(() -> new ValidationExecutionProperties.Kafka(false, null));
    }

    @Test
    @DisplayName("of Kafka accept a topic while publication is on")
    void kafkaAcceptsATopicWhileOn() {
        assertEquals("payments.validation.completed.v1",
                new ValidationExecutionProperties.Kafka(true, "payments.validation.completed.v1").topic());
    }

    @ParameterizedTest(name = "topic \"{0}\"")
    @ValueSource(strings = {"", " "})
    @DisplayName("of Kafka are refused with a blank topic while publication is on")
    void kafkaIsRefusedWithABlankTopic(String topic) {
        assertThrows(IllegalArgumentException.class, () -> new ValidationExecutionProperties.Kafka(true, topic));
    }

    @Test
    @DisplayName("of Kafka are refused without a topic while publication is on")
    void kafkaIsRefusedWithoutATopic() {
        assertThrows(IllegalArgumentException.class, () -> new ValidationExecutionProperties.Kafka(true, null));
    }
}
