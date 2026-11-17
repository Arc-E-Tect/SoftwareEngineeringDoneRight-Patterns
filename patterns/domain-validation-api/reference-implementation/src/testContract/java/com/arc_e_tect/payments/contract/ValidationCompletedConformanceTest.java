package com.arc_e_tect.payments.contract;

import com.arc_e_tect.payments.contract.microcks.AsyncConformanceHarness;
import com.arc_e_tect.validation.async.adapter.outbound.event.KafkaValidationCompletedPublisher;
import com.arc_e_tect.validation.async.application.domain.model.Outcome;
import com.arc_e_tect.validation.async.application.domain.model.ValidationCompleted;
import com.arc_e_tect.validation.async.application.domain.model.ValidationJob;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.connection.KafkaConnection;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The generated Microcks conformance test of the event published to payments.validation.completed.v1:
 * the starter's Kafka publisher, as the service runs it, publishes the completion of a job, and
 * Microcks checks it against the contract's AsyncAPI document.
 */
@DisplayName("The contract of the event on payments.validation.completed.v1")
class ValidationCompletedConformanceTest implements AsyncConformanceHarness {

    private static final String BROKER_ON_THE_NETWORK = "kafka:19092";

    private KafkaContainer kafka;
    private KafkaTemplate<String, String> producer;

    @Override
    public void wireBroker(MicrocksContainersEnsemble ensemble) {
        kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.2.2"))
                .withNetwork(ensemble.getNetwork())
                .withNetworkAliases("kafka")
                .withListener(BROKER_ON_THE_NETWORK);
        kafka.start();
        ensemble.withKafkaConnection(new KafkaConnection(BROKER_ON_THE_NETWORK));
        producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)));
    }

    @AfterAll
    void stopTheBroker() {
        producer.destroy();
        kafka.stop();
    }

    @Override
    public Path contractFile() {
        return Path.of("build/api-spec/domain-validation-api/asyncapi.yaml");
    }

    @Override
    public String endpoint(String suffixedChannel) {
        return "kafka://" + BROKER_ON_THE_NETWORK + "/" + suffixedChannel + "?startOffset=0";
    }

    @Override
    public void publishValidationCompleted(String suffixedChannel) {
        String validationId = UUID.randomUUID().toString();
        new KafkaValidationCompletedPublisher(producer, suffixedChannel, JsonMapper.builder().build())
                .publish(new ValidationCompleted(validationId, "name-iban-match", ValidationJob.Status.COMPLETED,
                        Optional.of(Outcome.VALID), Instant.now(), "/payments/v1/validations/name-iban-match/" + validationId));
        producer.flush();
    }
}
