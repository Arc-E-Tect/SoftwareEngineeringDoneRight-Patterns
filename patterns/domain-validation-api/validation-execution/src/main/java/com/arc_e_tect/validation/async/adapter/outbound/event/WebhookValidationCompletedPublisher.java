package com.arc_e_tect.validation.async.adapter.outbound.event;

import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationCompletedPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Posts the completion of a validation job to every subscriber of the {@code validationCompleted}
 * webhook, once and best effort: a subscriber that answers with an error, or cannot be reached, is
 * logged and skipped.
 */
public class WebhookValidationCompletedPublisher implements ValidationCompletedPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookValidationCompletedPublisher.class);

    private final RestClient client;
    private final List<URI> subscribers;
    private final JsonMapper json;

    public WebhookValidationCompletedPublisher(RestClient client, List<URI> subscribers, JsonMapper json) {
        this.client = Objects.requireNonNull(client, "client");
        this.subscribers = List.copyOf(Objects.requireNonNull(subscribers, "subscribers"));
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public void publish(ValidationCompleted completed) {
        String event = ValidationCompletedMessage.json(json, completed);
        for (URI subscriber : subscribers) {
            try {
                client.post().uri(subscriber).contentType(MediaType.APPLICATION_JSON).body(event).retrieve().toBodilessEntity();
            } catch (RestClientException undelivered) {
                LOG.warn("Webhook subscriber {} did not receive the completion of job {}: {}", subscriber, completed.validationId(), undelivered.getMessage());
            }
        }
    }
}
