package com.arc_e_tect.validation.async.configuration;

import com.arc_e_tect.validation.async.adapter.inbound.web.ValidationExecution;
import com.arc_e_tect.validation.async.adapter.outbound.event.KafkaValidationCompletedPublisher;
import com.arc_e_tect.validation.async.adapter.outbound.event.WebhookValidationCompletedPublisher;
import com.arc_e_tect.validation.async.adapter.outbound.memory.InMemoryValidationJobStore;
import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;
import com.arc_e_tect.validation.async.application.port.inbound.GetValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.SubmitValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.command.GetValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.JobOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.command.SubmitValidationJobCommand;
import com.arc_e_tect.validation.async.application.port.inbound.command.ValidationOutcome;
import com.arc_e_tect.validation.async.application.port.inbound.result.SubmitValidationJobResult;
import com.arc_e_tect.validation.async.application.port.inbound.result.ValidationJobView;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationCompletedPublisher;
import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@DisplayName("The auto-configuration of the asynchronous execution")
class ValidationExecutionAutoConfigurationTest {

    private final ValidationExecutionAutoConfiguration configuration = new ValidationExecutionAutoConfiguration();
    private final ValidationExecutionProperties properties = new ValidationExecutionProperties(Duration.ofSeconds(3),
            URI.create("https://problems.example.org/"), new ValidationExecutionProperties.Events(
            new ValidationExecutionProperties.Webhook(List.of(URI.create("https://subscriber.example.org/hooks")), Duration.ofMillis(200)),
            new ValidationExecutionProperties.Kafka(true, "payments.validation.completed.v1")));
    private final InMemoryValidationJobStore store = new InMemoryValidationJobStore();
    private final JsonMapper json = JsonMapper.builder().build();

    private static ObjectProvider<ValidationCompletedPublisher> publishers(ValidationCompletedPublisher... publishers) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        for (int index = 0; index < publishers.length; index++) {
            beans.addBean("publisher" + index, publishers[index]);
        }
        return beans.getBeanProvider(ValidationCompletedPublisher.class);
    }

    private SubmitValidationJobUseCase submit() {
        return configuration.submitValidationJobUseCase(store, properties, publishers());
    }

    /** The problem a call answers with; whether it answers with one at all is another test's concern. */
    private static ErrorResponseException refusal(Runnable call) {
        try {
            call.run();
        } catch (ErrorResponseException refused) {
            return refused;
        }
        throw new IllegalStateException("The call answered without a problem");
    }

    @AfterEach
    void forgetTheRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("stores jobs in memory, unless the bounded context supplies a store")
    void storesJobsInMemory() {
        assertInstanceOf(InMemoryValidationJobStore.class, configuration.validationJobStore());
    }

    @Test
    @DisplayName("fails a job that fails unexpectedly with the internal-server-error problem of the configured base")
    void failsAnUnexpectedFailureWithTheInternalServerErrorProblem() {
        assertEquals(new JobProblem(URI.create("https://problems.example.org/internal-server-error"), "Internal Server Error", 500,
                "An unexpected error occurred while running the validation."), configuration.unexpectedFailure(properties));
    }

    @Test
    @DisplayName("submits jobs to the store")
    void submitsJobsToTheStore() {
        ValidationJobView job = ((SubmitValidationJobResult.Accepted) submit().submit(new SubmitValidationJobCommand(
                "name-iban-match", "key", "request", "/validations",
                () -> new JobOutcome.Completed("result", ValidationOutcome.VALID)))).job();

        assertEquals(job.validationId(), store.find("name-iban-match", job.validationId()).orElseThrow().validationId());
    }

    @Test
    @DisplayName("publishes the completion of a job to the publishers there are")
    void publishesTheCompletion() throws Exception {
        CompletableFuture<ValidationCompleted> published = new CompletableFuture<>();
        SubmitValidationJobUseCase submit = configuration.submitValidationJobUseCase(store, properties, publishers(published::complete));
        ValidationJobView job = ((SubmitValidationJobResult.Accepted) submit.submit(new SubmitValidationJobCommand(
                "name-iban-match", "key", "request", "/validations",
                () -> new JobOutcome.Completed("result", ValidationOutcome.VALID)))).job();

        assertEquals(job.validationId(), published.get(5, TimeUnit.SECONDS).validationId());
    }

    @Test
    @DisplayName("finds jobs in the store")
    void findsJobsInTheStore() {
        GetValidationJobUseCase get = configuration.getValidationJobUseCase(store);

        assertEquals(Optional.empty(), get.find(new GetValidationJobCommand("name-iban-match", "id")));
    }

    @Test
    @DisplayName("answers Retry-After as configured")
    void answersRetryAfterAsConfigured() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/payments/v1/validations/name-iban-match")));
        ValidationExecution execution = configuration.validationExecution(
                submit(), configuration.getValidationJobUseCase(store), properties);

        ResponseEntity<String> response = execution.run("name-iban-match", "respond-async", "key", "request", () -> "result",
                result -> ValidationOutcome.VALID);

        assertEquals("3", response.getHeaders().getFirst("Retry-After"));
    }

    @Test
    @DisplayName("answers problems of the configured base")
    void answersProblemsOfTheConfiguredBase() {
        ValidationExecution execution = configuration.validationExecution(
                submit(), configuration.getValidationJobUseCase(store), properties);

        ErrorResponseException notFound = refusal(() -> execution.job("name-iban-match", "id", view -> view));

        assertEquals(URI.create("https://problems.example.org/validation-job-not-found"), notFound.getBody().getType());
    }

    @Nested
    @DisplayName("for the webhook")
    class ForTheWebhook {

        @Test
        @DisplayName("delivers to the configured subscribers")
        void deliversToTheSubscribers() {
            assertInstanceOf(WebhookValidationCompletedPublisher.class, configuration.webhookValidationCompletedPublisher(properties, json));
        }

        @Test
        @DisplayName("gives up on a subscriber that does not answer within the timeout")
        void givesUpOnASlowSubscriber() throws IOException {
            HttpServer slow = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            slow.createContext("/", exchange -> {
                try {
                    Thread.sleep(Duration.ofSeconds(10));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                exchange.close();
            });
            slow.start();
            try {
                RestClient client = ValidationExecutionAutoConfiguration.webhookClient(Duration.ofMillis(200));
                URI subscriber = URI.create("http://localhost:" + slow.getAddress().getPort() + "/hooks");

                assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertThrows(ResourceAccessException.class,
                        () -> client.post().uri(subscriber).body("{}").retrieve().toBodilessEntity()));
            } finally {
                slow.stop(0);
            }
        }
    }

    @Nested
    @DisplayName("for Kafka")
    class ForKafka {

        private final DefaultKafkaProducerFactory<Object, Object> producers =
                new DefaultKafkaProducerFactory<>(Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"));

        @Test
        @DisplayName("publishes to the configured topic")
        void publishesToTheTopic() {
            assertInstanceOf(KafkaValidationCompletedPublisher.class,
                    new ValidationExecutionAutoConfiguration.KafkaPublication().kafkaValidationCompletedPublisher(producers, properties, json));
        }

        @Test
        @DisplayName("sends the key and the event as text, whatever the application's serializers")
        void sendsText() {
            KafkaTemplate<String, String> template = ValidationExecutionAutoConfiguration.KafkaPublication.template(producers);

            assertEquals(List.of(StringSerializer.class, StringSerializer.class), List.of(
                    template.getProducerFactory().getConfigurationProperties().get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG),
                    template.getProducerFactory().getConfigurationProperties().get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)));
        }
    }
}
