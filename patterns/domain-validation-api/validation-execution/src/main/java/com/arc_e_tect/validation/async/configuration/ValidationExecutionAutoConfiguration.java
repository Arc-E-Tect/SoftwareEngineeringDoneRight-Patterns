package com.arc_e_tect.validation.async.configuration;

import com.arc_e_tect.validation.async.adapter.inbound.web.ValidationExecution;
import com.arc_e_tect.validation.async.adapter.inbound.web.ValidationExecutionProblems;
import com.arc_e_tect.validation.async.adapter.outbound.event.KafkaValidationCompletedPublisher;
import com.arc_e_tect.validation.async.adapter.outbound.event.WebhookValidationCompletedPublisher;
import com.arc_e_tect.validation.async.adapter.outbound.memory.InMemoryValidationJobStore;
import com.arc_e_tect.validation.async.application.domain.model.JobProblem;
import com.arc_e_tect.validation.async.application.domain.service.GetValidationJobService;
import com.arc_e_tect.validation.async.application.domain.service.SubmitValidationJobService;
import com.arc_e_tect.validation.async.application.port.inbound.GetValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.inbound.SubmitValidationJobUseCase;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationCompletedPublisher;
import com.arc_e_tect.validation.async.application.port.outbound.ValidationJobStore;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sets up the asynchronous execution in a servlet web application: the use cases, the
 * {@link ValidationExecution} a validation's controller uses, and an in-memory job store unless the
 * bounded context supplies its own {@link ValidationJobStore}. Jobs run on virtual threads, of the
 * executor {@value #JOB_EXECUTOR} unless the bounded context supplies its own of that name; Spring
 * closes it when the application stops, which refuses new jobs and waits for the running ones.
 *
 * <p>The completion of a job is posted to the webhook's subscribers, and sent to Kafka when the
 * bounded context has Kafka and turns publication on.
 */
@AutoConfiguration(afterName = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ValidationExecutionProperties.class)
public class ValidationExecutionAutoConfiguration {

    /** The name of the executor jobs run on. */
    public static final String JOB_EXECUTOR = "validationJobExecutor";

    @Bean
    @ConditionalOnMissingBean
    public ValidationJobStore validationJobStore() {
        return new InMemoryValidationJobStore();
    }

    /** A virtual thread per job; closed with the application context, which waits for the jobs that run. */
    @Bean(name = JOB_EXECUTOR, destroyMethod = "close")
    @ConditionalOnMissingBean(name = JOB_EXECUTOR)
    public ExecutorService validationJobExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public SubmitValidationJobUseCase submitValidationJobUseCase(ValidationJobStore store, @Qualifier(JOB_EXECUTOR) Executor executor,
                                                                 ValidationExecutionProperties properties,
                                                                 ObjectProvider<ValidationCompletedPublisher> publishers) {
        return new SubmitValidationJobService(store, executor, Clock.systemUTC(), publishers.orderedStream().toList(),
                unexpectedFailure(properties));
    }

    @Bean
    public GetValidationJobUseCase getValidationJobUseCase(ValidationJobStore store) {
        return new GetValidationJobService(store);
    }

    @Bean
    public ValidationExecution validationExecution(SubmitValidationJobUseCase submit, GetValidationJobUseCase get,
                                                   ValidationExecutionProperties properties) {
        return new ValidationExecution(submit, get, new ValidationExecutionProblems(properties.problemTypes()), properties.retryAfter());
    }

    @Bean
    public WebhookValidationCompletedPublisher webhookValidationCompletedPublisher(ValidationExecutionProperties properties, JsonMapper json) {
        ValidationExecutionProperties.Webhook webhook = properties.events().webhook();
        return new WebhookValidationCompletedPublisher(webhookClient(webhook.timeout()), webhook.subscribers(), json);
    }

    /** The problem of a job whose validation failed in a way it did not answer with a problem of its own. */
    JobProblem unexpectedFailure(ValidationExecutionProperties properties) {
        return new JobProblem(properties.problemTypes().resolve("internal-server-error"), "Internal Server Error", 500,
                "An unexpected error occurred while running the validation.");
    }

    /** A client that gives up on a subscriber which does not connect, or answer, within the timeout. */
    static RestClient webhookClient(Duration timeout) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
        requests.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(requests).build();
    }

    /** Publication to Kafka, through the producer factory of Spring Boot's Kafka auto-configuration. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(KafkaOperations.class)
    @ConditionalOnBooleanProperty("validation.execution.events.kafka.enabled")
    static class KafkaPublication {

        @Bean
        KafkaValidationCompletedPublisher kafkaValidationCompletedPublisher(ProducerFactory<?, ?> producers,
                                                                            ValidationExecutionProperties properties, JsonMapper json) {
            return new KafkaValidationCompletedPublisher(template(producers), properties.events().kafka().topic(), json);
        }

        /** A template that sends the key and the event as text, whatever serializers the application configured. */
        @SuppressWarnings("unchecked")
        static KafkaTemplate<String, String> template(ProducerFactory<?, ?> producers) {
            return new KafkaTemplate<>((ProducerFactory<String, String>) producers, Map.of(
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class));
        }
    }
}
