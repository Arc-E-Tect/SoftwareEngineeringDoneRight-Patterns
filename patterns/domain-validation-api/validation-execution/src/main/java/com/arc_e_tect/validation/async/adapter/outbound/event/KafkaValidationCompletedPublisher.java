package com.arc_e_tect.validation.async.adapter.outbound.event;

import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationCompletedPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaOperations;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

/**
 * Sends the completion of a validation job to its topic, keyed by the job's identifier so that the
 * events of one job stay in order; best effort: a send that fails is logged.
 */
public class KafkaValidationCompletedPublisher implements ValidationCompletedPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaValidationCompletedPublisher.class);

    private final KafkaOperations<String, String> kafka;
    private final String topic;
    private final JsonMapper json;

    public KafkaValidationCompletedPublisher(KafkaOperations<String, String> kafka, String topic, JsonMapper json) {
        this.kafka = Objects.requireNonNull(kafka, "kafka");
        this.topic = Objects.requireNonNull(topic, "topic");
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public void publish(ValidationCompleted completed) {
        String id = completed.validationId();
        try {
            kafka.send(topic, id, ValidationCompletedMessage.json(json, completed)).exceptionally(failure -> {
                LOG.warn("Topic {} did not receive the completion of job {}: {}", topic, id, failure.getMessage());
                return null;
            });
        } catch (RuntimeException unsent) {
            LOG.warn("Topic {} did not receive the completion of job {}: {}", topic, id, unsent.getMessage());
        }
    }
}
