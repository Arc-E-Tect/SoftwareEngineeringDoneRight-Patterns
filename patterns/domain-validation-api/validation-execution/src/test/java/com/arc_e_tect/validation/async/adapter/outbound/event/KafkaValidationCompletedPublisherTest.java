package com.arc_e_tect.validation.async.adapter.outbound.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CompletableFuture;

import static com.arc_e_tect.validation.async.adapter.outbound.event.ValidationCompletedMessageTest.COMPLETED;
import static com.arc_e_tect.validation.async.adapter.outbound.event.ValidationCompletedMessageTest.COMPLETED_JSON;
import static com.arc_e_tect.validation.async.adapter.outbound.event.ValidationCompletedMessageTest.ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("The Kafka publisher of a validation's completion")
class KafkaValidationCompletedPublisherTest {

    private static final String TOPIC = "payments.validation.completed.v1";

    private final JsonMapper json = JsonMapper.builder().build();
    @SuppressWarnings("unchecked")
    private final KafkaOperations<String, String> kafka = mock(KafkaOperations.class);
    private final KafkaValidationCompletedPublisher publisher = new KafkaValidationCompletedPublisher(kafka, TOPIC, json);

    @Test
    @DisplayName("sends the event to the topic, keyed by the job's identifier")
    void sendsTheEvent() {
        when(kafka.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        publisher.publish(COMPLETED);

        verify(kafka).send(TOPIC, ID, COMPLETED_JSON);
    }

    @Nested
    @DisplayName("publishes best effort")
    class BestEffort {

        @Test
        @DisplayName("and lets a send that fails later pass")
        void whenTheSendFailsLater() {
            when(kafka.send(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new KafkaException("The broker is unavailable")));

            assertDoesNotThrow(() -> publisher.publish(COMPLETED));
        }

        @Test
        @DisplayName("and lets a send that fails at once pass")
        void whenTheSendFailsAtOnce() {
            when(kafka.send(any(), any(), any())).thenThrow(new KafkaException("Topic not present in metadata"));

            assertDoesNotThrow(() -> publisher.publish(COMPLETED));
        }
    }

    @Nested
    @DisplayName("is refused")
    class IsRefused {

        @Test
        @DisplayName("without Kafka")
        void withoutKafka() {
            assertThrows(NullPointerException.class, () -> new KafkaValidationCompletedPublisher(null, TOPIC, json));
        }

        @Test
        @DisplayName("without a topic")
        void withoutATopic() {
            assertThrows(NullPointerException.class, () -> new KafkaValidationCompletedPublisher(kafka, null, json));
        }

        @Test
        @DisplayName("without a JSON mapper")
        void withoutAJsonMapper() {
            assertThrows(NullPointerException.class, () -> new KafkaValidationCompletedPublisher(kafka, TOPIC, null));
        }
    }
}
