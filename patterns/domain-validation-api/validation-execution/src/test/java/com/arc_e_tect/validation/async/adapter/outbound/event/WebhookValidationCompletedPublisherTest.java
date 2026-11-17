package com.arc_e_tect.validation.async.adapter.outbound.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.arc_e_tect.validation.async.adapter.outbound.event.ValidationCompletedMessageTest.COMPLETED;
import static com.arc_e_tect.validation.async.adapter.outbound.event.ValidationCompletedMessageTest.COMPLETED_JSON;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@DisplayName("The webhook publisher of a validation's completion")
class WebhookValidationCompletedPublisherTest {

    private static final URI FIRST = URI.create("https://subscriber.example.org/hooks/validation-completed");
    private static final URI SECOND = URI.create("https://another.example.org/validations");

    private final JsonMapper json = JsonMapper.builder().build();
    private final RestClient.Builder client = RestClient.builder();
    private final MockRestServiceServer subscribers = MockRestServiceServer.bindTo(client).build();

    private WebhookValidationCompletedPublisher publisher(URI... subscribed) {
        return new WebhookValidationCompletedPublisher(client.build(), List.of(subscribed), json);
    }

    private void expectDelivery(URI subscriber) {
        subscribers.expect(requestTo(subscriber))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(COMPLETED_JSON, JsonCompareMode.STRICT))
                .andRespond(withNoContent());
    }

    @Test
    @DisplayName("posts the event to every subscriber")
    void postsToEverySubscriber() {
        expectDelivery(FIRST);
        expectDelivery(SECOND);

        publisher(FIRST, SECOND).publish(COMPLETED);

        subscribers.verify();
    }

    @Test
    @DisplayName("posts nothing without subscribers")
    void postsNothingWithoutSubscribers() {
        publisher().publish(COMPLETED);

        subscribers.verify();
    }

    @Nested
    @DisplayName("delivers best effort")
    class BestEffort {

        @Test
        @DisplayName("and still posts to the next subscriber when one answers with an error")
        void afterAnError() {
            subscribers.expect(requestTo(FIRST)).andRespond(withServerError());
            expectDelivery(SECOND);

            publisher(FIRST, SECOND).publish(COMPLETED);

            subscribers.verify();
        }

        @Test
        @DisplayName("and still posts to the next subscriber when one cannot be reached")
        void afterAnUnreachableSubscriber() {
            subscribers.expect(requestTo(FIRST)).andRespond(withException(new IOException("Connection refused")));
            expectDelivery(SECOND);

            publisher(FIRST, SECOND).publish(COMPLETED);

            subscribers.verify();
        }
    }

    @Nested
    @DisplayName("is refused")
    class IsRefused {

        @Test
        @DisplayName("without a client")
        void withoutAClient() {
            List<URI> subscribed = List.of(FIRST);

            assertThrows(NullPointerException.class, () -> new WebhookValidationCompletedPublisher(null, subscribed, json));
        }

        @Test
        @DisplayName("without subscribers")
        void withoutSubscribers() {
            RestClient built = client.build();

            assertThrows(NullPointerException.class, () -> new WebhookValidationCompletedPublisher(built, null, json));
        }

        @Test
        @DisplayName("without a JSON mapper")
        void withoutAJsonMapper() {
            RestClient built = client.build();
            List<URI> subscribed = List.of(FIRST);

            assertThrows(NullPointerException.class, () -> new WebhookValidationCompletedPublisher(built, subscribed, null));
        }
    }
}
